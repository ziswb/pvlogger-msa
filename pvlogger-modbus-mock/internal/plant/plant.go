// Package plant is the synthetic PV plant behind the mock server and the
// history backfill (ADR-0025): a deterministic function of time, so every
// process that asks for the same minute gets the same snapshot.
package plant

import (
	"math"
	"sync"
	"time"
)

// Defaults: a generic Central European plant, not any real site.
const (
	DefaultLatitudeDeg  = 47.0
	DefaultLongitudeDeg = 10.0
	DefaultPeakPowerW   = 10_000.0
	panelTiltDeg        = 30.0
	panelAzimuthDeg     = 180.0 // south
	// Share of peak power a clean module delivers in full sun (temperature,
	// inverter and wiring losses).
	performanceRatio = 0.85
)

// Config describes the plant. Zero values get the defaults in New.
type Config struct {
	// Location is the zone whose local midnight resets the daily counter
	// (the chart's global.timezone).
	Location     *time.Location
	LatitudeDeg  float64
	LongitudeDeg float64
	PeakPowerW   float64
	// Origin is where the lifetime counters stand at 0 (local midnight of its
	// day). Default: 2025-01-01. Times before it read 0.
	Origin time.Time
}

// Plant answers snapshots for any minute.
type Plant struct {
	cfg Config

	mu   sync.Mutex
	days map[time.Time][]energy // local midnight → energy before each minute of that day, plus the day's total
}

// energy is what flowed over some span of minutes, in kWh.
type energy struct{ yield, exported, imported float64 }

func (e energy) plus(o energy) energy {
	return energy{e.yield + o.yield, e.exported + o.exported, e.imported + o.imported}
}

// Snapshot is the plant's state during one minute, in physical units.
// Counters hold the energy of all minutes before Time.
type Snapshot struct {
	Time        time.Time
	PVPowerW    float64
	LoadPowerW  float64
	GridImportW float64 // load the PV cannot cover
	GridExportW float64 // PV the load does not use

	YieldTotalKWh    float64 // inverter lifetime yield (32106)
	YieldTodayKWh    float64 // inverter daily yield since local midnight (32114)
	ExportedTotalKWh float64 // meter, energy fed into the grid (37119)
	ImportedTotalKWh float64 // meter, energy drawn from the grid (37121)
}

// New returns a plant for cfg.
func New(cfg Config) *Plant {
	if cfg.Location == nil {
		cfg.Location = time.UTC
	}
	if cfg.LatitudeDeg == 0 {
		cfg.LatitudeDeg = DefaultLatitudeDeg
	}
	if cfg.LongitudeDeg == 0 {
		cfg.LongitudeDeg = DefaultLongitudeDeg
	}
	if cfg.PeakPowerW == 0 {
		cfg.PeakPowerW = DefaultPeakPowerW
	}
	if cfg.Origin.IsZero() {
		cfg.Origin = time.Date(2025, 1, 1, 0, 0, 0, 0, cfg.Location)
	}
	cfg.Origin = localMidnight(cfg.Origin.In(cfg.Location))
	return &Plant{cfg: cfg, days: map[time.Time][]energy{}}
}

// PeakPowerW is the plant's rated (peak) power.
func (p *Plant) PeakPowerW() float64 { return p.cfg.PeakPowerW }

// Location is the zone of the plant's local days.
func (p *Plant) Location() *time.Location { return p.cfg.Location }

// At returns the snapshot of the minute that contains t.
func (p *Plant) At(t time.Time) Snapshot {
	minute := t.In(p.cfg.Location).Truncate(time.Minute)
	s := p.power(minute)
	midnight := localMidnight(minute)
	beforeToday := p.energyBefore(midnight)
	total := beforeToday.plus(p.day(midnight)[int(minute.Sub(midnight)/time.Minute)])
	s.YieldTotalKWh = total.yield
	s.YieldTodayKWh = total.yield - beforeToday.yield
	s.ExportedTotalKWh = total.exported
	s.ImportedTotalKWh = total.imported
	return s
}

// power is the snapshot's power part for one local minute.
func (p *Plant) power(minute time.Time) Snapshot {
	pv := p.clearSkyPowerW(minute) * skyFactor(minute)
	load := loadPowerW(minute)
	return Snapshot{
		Time:        minute,
		PVPowerW:    pv,
		LoadPowerW:  load,
		GridImportW: math.Max(load-pv, 0),
		GridExportW: math.Max(pv-load, 0),
	}
}

// energyBefore sums whole days from the origin up to the local midnight m,
// always in the same order, so the result does not depend on what was asked
// before.
func (p *Plant) energyBefore(m time.Time) energy {
	var sum energy
	for day := p.cfg.Origin; day.Before(m); day = nextMidnight(day) {
		sum = sum.plus(p.dayTotal(day))
	}
	return sum
}

func (p *Plant) dayTotal(midnight time.Time) energy {
	d := p.day(midnight)
	return d[len(d)-1]
}

// day returns the running sum of one local day: entry i is the energy of the
// minutes before minute i, the last entry the whole day's. Computed once per
// day and cached; minutes before the origin count as nothing.
func (p *Plant) day(midnight time.Time) []energy {
	p.mu.Lock()
	cached, ok := p.days[midnight]
	p.mu.Unlock()
	if ok {
		return cached
	}
	end := nextMidnight(midnight)
	sums := make([]energy, 0, 25*60+1)
	var e energy
	for m := midnight; m.Before(end); m = m.Add(time.Minute) {
		sums = append(sums, e)
		if !m.Before(p.cfg.Origin) {
			s := p.power(m)
			e = e.plus(energy{s.PVPowerW / 60_000, s.GridExportW / 60_000, s.GridImportW / 60_000})
		}
	}
	sums = append(sums, e)
	p.mu.Lock()
	p.days[midnight] = sums
	p.mu.Unlock()
	return sums
}

func localMidnight(t time.Time) time.Time {
	return time.Date(t.Year(), t.Month(), t.Day(), 0, 0, 0, 0, t.Location())
}

// nextMidnight steps by calendar day, so DST days get their 23 or 25 hours.
func nextMidnight(midnight time.Time) time.Time {
	return time.Date(midnight.Year(), midnight.Month(), midnight.Day()+1, 0, 0, 0, 0, midnight.Location())
}

// clearSkyPowerW is the plant's output under a clear sky at t: direct sun on
// the tilted panel, attenuated by the air mass, plus a little diffuse light.
func (p *Plant) clearSkyPowerW(t time.Time) float64 {
	elevation, azimuth := sunPosition(t, p.cfg.LatitudeDeg, p.cfg.LongitudeDeg)
	if elevation <= 0 {
		return 0
	}
	sinElev := math.Sin(elevation)
	tilt := rad(panelTiltDeg)
	cosIncidence := sinElev*math.Cos(tilt) + math.Cos(elevation)*math.Sin(tilt)*math.Cos(azimuth-rad(panelAzimuthDeg))
	airMass := 1 / sinElev
	direct := math.Pow(0.7, math.Pow(airMass, 0.678)) * math.Max(cosIncidence, 0)
	diffuse := 0.1 * sinElev
	irradiance := (direct + diffuse) / 0.8 // 0.8: direct+diffuse at a high summer sun, the module's reference
	return math.Min(p.cfg.PeakPowerW, p.cfg.PeakPowerW*performanceRatio*irradiance)
}

// sunPosition returns the sun's elevation and azimuth (radians, azimuth from
// north, clockwise) at t for the given site. Standard approximations
// (declination and equation of time from the day of the year) — accurate to
// well under a degree, which is plenty for a synthetic plant.
func sunPosition(t time.Time, latDeg, lonDeg float64) (elevation, azimuth float64) {
	utc := t.UTC()
	dayAngle := 2 * math.Pi / 365 * (float64(utc.YearDay()-1) + (float64(utc.Hour())-12)/24)
	declination := 0.006918 - 0.399912*math.Cos(dayAngle) + 0.070257*math.Sin(dayAngle) -
		0.006758*math.Cos(2*dayAngle) + 0.000907*math.Sin(2*dayAngle) -
		0.002697*math.Cos(3*dayAngle) + 0.00148*math.Sin(3*dayAngle)
	eqTimeMin := 229.18 * (0.000075 + 0.001868*math.Cos(dayAngle) - 0.032077*math.Sin(dayAngle) -
		0.014615*math.Cos(2*dayAngle) - 0.040849*math.Sin(2*dayAngle))
	utcMin := float64(utc.Hour()*60+utc.Minute()) + float64(utc.Second())/60
	solarMin := utcMin + eqTimeMin + 4*lonDeg
	hourAngle := rad(solarMin/4 - 180)
	lat := rad(latDeg)
	sinElev := math.Sin(lat)*math.Sin(declination) + math.Cos(lat)*math.Cos(declination)*math.Cos(hourAngle)
	elevation = math.Asin(sinElev)
	cosAz := (math.Sin(declination) - math.Sin(lat)*sinElev) / (math.Cos(lat) * math.Cos(elevation))
	azimuth = math.Acos(math.Max(-1, math.Min(1, cosAz)))
	if hourAngle > 0 {
		azimuth = 2*math.Pi - azimuth
	}
	return elevation, azimuth
}

func rad(deg float64) float64 { return deg * math.Pi / 180 }

// House load profile: a base load, morning, midday and evening peaks, and a
// few appliance blocks (oven, washing machine) placed per day.
const (
	baseLoadW         = 230.0
	minuteNoiseW      = 30.0
	appliancesPerDay  = 2
	applianceMinW     = 1500.0
	applianceMaxW     = 2500.0
	applianceMinMin   = 20
	applianceMaxMin   = 60
	applianceFirstMin = 9 * 60
	applianceLastMin  = 21 * 60
)

var loadPeaks = []struct{ centreMin, sigmaMin, powerW float64 }{
	{7*60 + 15, 35, 700},
	{12*60 + 15, 40, 300},
	{19*60 + 15, 70, 900},
}

// loadPowerW is the house consumption during the local minute t.
func loadPowerW(t time.Time) float64 {
	minuteOfDay := float64(t.Hour()*60 + t.Minute())
	load := baseLoadW
	for _, peak := range loadPeaks {
		d := (minuteOfDay - peak.centreMin) / peak.sigmaMin
		load += peak.powerW * math.Exp(-d*d/2)
	}
	day := dayKey(t)
	for i := uint64(0); i < appliancesPerDay; i++ {
		start := applianceFirstMin + unit(day, 10+i)*float64(applianceLastMin-applianceFirstMin)
		length := applianceMinMin + unit(day, 20+i)*float64(applianceMaxMin-applianceMinMin)
		if minuteOfDay >= start && minuteOfDay < start+length {
			load += applianceMinW + unit(day, 30+i)*(applianceMaxW-applianceMinW)
		}
	}
	load += (unit(minuteKey(t), 1)*2 - 1) * minuteNoiseW
	return load
}

// dayKey and minuteKey identify a local day and a minute; unit turns a key
// and a salt into a reproducible number in [0, 1). This is the plant's only
// source of variety: no random generator, so every process agrees (ADR-0025).
func dayKey(t time.Time) uint64 {
	return uint64(t.Year())*10_000 + uint64(t.Month())*100 + uint64(t.Day())
}

func minuteKey(t time.Time) uint64 { return uint64(t.Unix() / 60) }

func unit(key, salt uint64) float64 {
	return float64(splitmix64(key*0x9E3779B97F4A7C15^salt)>>11) / (1 << 53)
}

func splitmix64(x uint64) uint64 {
	x += 0x9E3779B97F4A7C15
	x = (x ^ (x >> 30)) * 0xBF58476D1CE4E5B9
	x = (x ^ (x >> 27)) * 0x94D049BB133111EB
	return x ^ (x >> 31)
}

// Weather: each local day is sunny, mixed or overcast, more often overcast in
// winter. skyFactor is the share of the clear-sky output that gets through.
type weather int

const (
	sunny weather = iota
	mixed
	overcast
)

const cloudBlockMin = 10 // a mixed day's clouds change every 10 minutes

func dayWeather(t time.Time) weather {
	// +1 at midsummer, -1 at midwinter.
	season := math.Cos(2 * math.Pi * float64(t.YearDay()-172) / 365)
	pSunny := 0.325 + 0.125*season
	pOvercast := 0.30 - 0.15*season
	switch r := unit(dayKey(t), 1); {
	case r < pSunny:
		return sunny
	case r < 1-pOvercast:
		return mixed
	default:
		return overcast
	}
}

func skyFactor(t time.Time) float64 {
	day := dayKey(t)
	switch dayWeather(t) {
	case sunny:
		return 0.95 + 0.05*unit(day, 2) // a little haze
	case overcast:
		base := 0.12 + 0.18*unit(day, 3)
		return base * (1 + 0.15*math.Sin(float64(t.Hour()*60+t.Minute())/37))
	default:
		// Clouds in blocks, linearly blended into the next block.
		minute := minuteKey(t)
		block := minute / cloudBlockMin
		frac := float64(minute%cloudBlockMin) / cloudBlockMin
		return (1-frac)*cloudCover(block) + frac*cloudCover(block+1)
	}
}

// cloudCover is the sky factor of one 10-minute block on a mixed day: the sun
// is out a bit more than half of the time.
func cloudCover(block uint64) float64 {
	if v := unit(block, 4); v < 0.45 {
		return 0.2 + 0.3*v/0.45
	}
	return 1
}
