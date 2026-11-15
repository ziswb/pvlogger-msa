package plant_test

import (
	"math"
	"testing"
	"time"

	"pvlogger-modbus-mock/internal/plant"
)

func zone(t *testing.T) *time.Location {
	t.Helper()
	// Any zone with DST works; Europe/Berlin is the generic Central European one.
	loc, err := time.LoadLocation("Europe/Berlin")
	if err != nil {
		t.Fatal(err)
	}
	return loc
}

func newPlant(loc *time.Location) *plant.Plant {
	return plant.New(plant.Config{Location: loc})
}

func TestSamePlantSameTimeGivesSameSnapshot(t *testing.T) {
	loc := zone(t)
	at := time.Date(2026, 6, 21, 13, 37, 0, 0, loc)
	a := newPlant(loc).At(at)
	b := newPlant(loc).At(at)
	if a != b {
		t.Fatalf("snapshots differ:\n%+v\n%+v", a, b)
	}
}

func TestNoPVAtNightSomeAtSummerNoonNeverAbovePeak(t *testing.T) {
	loc := zone(t)
	p := newPlant(loc)
	if got := p.At(time.Date(2026, 6, 21, 0, 0, 0, 0, loc)).PVPowerW; got != 0 {
		t.Errorf("PV at midnight = %v W, want 0", got)
	}
	if got := p.At(time.Date(2026, 1, 15, 21, 0, 0, 0, loc)).PVPowerW; got != 0 {
		t.Errorf("PV on a winter evening = %v W, want 0", got)
	}
	noon := time.Date(2026, 6, 21, 13, 0, 0, 0, loc)
	if got := p.At(noon).PVPowerW; got <= 0 {
		t.Errorf("PV at summer noon = %v W, want > 0", got)
	}
	for m := 0; m < 24*60; m++ {
		at := time.Date(2026, 6, 21, 0, m, 0, 0, loc)
		if got := p.At(at).PVPowerW; got > plant.DefaultPeakPowerW {
			t.Fatalf("PV at %v = %v W, above peak %v W", at, got, plant.DefaultPeakPowerW)
		}
	}
}

func TestPowerBalances(t *testing.T) {
	loc := zone(t)
	p := newPlant(loc)
	start := time.Date(2026, 4, 10, 0, 0, 0, 0, loc)
	for m := 0; m < 2*24*60; m += 7 {
		s := p.At(start.Add(time.Duration(m) * time.Minute))
		if s.LoadPowerW <= 0 {
			t.Fatalf("%v: load %v W, want > 0", s.Time, s.LoadPowerW)
		}
		if s.GridImportW < 0 || s.GridExportW < 0 || (s.GridImportW > 0 && s.GridExportW > 0) {
			t.Fatalf("%v: import %v W, export %v W: both must be >= 0 and not both > 0", s.Time, s.GridImportW, s.GridExportW)
		}
		if got := s.PVPowerW - s.GridExportW + s.GridImportW; math.Abs(got-s.LoadPowerW) > 1e-6 {
			t.Fatalf("%v: PV - export + import = %v W, load %v W", s.Time, got, s.LoadPowerW)
		}
	}
}

// A counter at minute t holds the energy of all minutes before t.
func TestCountersSumThePowerOfTheMinutesBetween(t *testing.T) {
	loc := zone(t)
	p := newPlant(loc)
	from := time.Date(2026, 5, 3, 9, 30, 0, 0, loc)
	to := from.Add(26 * time.Hour)
	var yield, exported, imported float64
	prev := p.At(from)
	for at := from; at.Before(to); at = at.Add(time.Minute) {
		s := p.At(at)
		yield += s.PVPowerW / 60_000
		exported += s.GridExportW / 60_000
		imported += s.GridImportW / 60_000
		if s.YieldTotalKWh < prev.YieldTotalKWh || s.ExportedTotalKWh < prev.ExportedTotalKWh || s.ImportedTotalKWh < prev.ImportedTotalKWh {
			t.Fatalf("%v: a counter decreased", at)
		}
		prev = s
	}
	start, end := p.At(from), p.At(to)
	for name, c := range map[string][2]float64{
		"yield":    {end.YieldTotalKWh - start.YieldTotalKWh, yield},
		"exported": {end.ExportedTotalKWh - start.ExportedTotalKWh, exported},
		"imported": {end.ImportedTotalKWh - start.ImportedTotalKWh, imported},
	} {
		if math.Abs(c[0]-c[1]) > 1e-6 {
			t.Errorf("%s counter grew by %v kWh, the minutes add up to %v kWh", name, c[0], c[1])
		}
	}
}

func TestDailyYieldStartsAtMidnightAndFollowsTheTotal(t *testing.T) {
	loc := zone(t)
	p := newPlant(loc)
	midnight := p.At(time.Date(2026, 5, 4, 0, 0, 0, 0, loc))
	if midnight.YieldTodayKWh != 0 {
		t.Errorf("daily yield at midnight = %v kWh, want 0", midnight.YieldTodayKWh)
	}
	evening := p.At(time.Date(2026, 5, 4, 20, 0, 0, 0, loc))
	if want := evening.YieldTotalKWh - midnight.YieldTotalKWh; math.Abs(evening.YieldTodayKWh-want) > 1e-6 || want <= 0 {
		t.Errorf("daily yield at 20:00 = %v kWh, total grew by %v kWh since midnight", evening.YieldTodayKWh, want)
	}
}

func dailyYieldsKWh(p *plant.Plant, loc *time.Location, year int, month time.Month) []float64 {
	var yields []float64
	for day := 1; ; day++ {
		midnight := time.Date(year, month, day, 0, 0, 0, 0, loc)
		if midnight.Month() != month {
			return yields
		}
		next := time.Date(year, month, day+1, 0, 0, 0, 0, loc)
		yields = append(yields, p.At(next).YieldTotalKWh-p.At(midnight).YieldTotalKWh)
	}
}

func sum(xs []float64) (s float64) {
	for _, x := range xs {
		s += x
	}
	return s
}

func TestSummerYieldsMoreThanWinter(t *testing.T) {
	loc := zone(t)
	p := newPlant(loc)
	june, december := sum(dailyYieldsKWh(p, loc, 2025, time.June)), sum(dailyYieldsKWh(p, loc, 2025, time.December))
	if june <= 2*december {
		t.Errorf("June %v kWh, December %v kWh: want June well above December", june, december)
	}
}

func TestDailyYieldsVaryWithTheWeather(t *testing.T) {
	loc := zone(t)
	p := newPlant(loc)
	for _, month := range []time.Month{time.April, time.July, time.October} {
		yields := dailyYieldsKWh(p, loc, 2025, month)
		lo, hi := yields[0], yields[0]
		for _, y := range yields {
			lo, hi = math.Min(lo, y), math.Max(hi, y)
		}
		if hi < 3*lo {
			t.Errorf("%v: daily yields %.1f..%.1f kWh, want sunny days at least 3x the dullest", month, lo, hi)
		}
	}
}

func TestAnnualTotalsArePlausible(t *testing.T) {
	loc := zone(t)
	p := newPlant(loc)
	start, end := p.At(time.Date(2025, 1, 1, 0, 0, 0, 0, loc)), p.At(time.Date(2026, 1, 1, 0, 0, 0, 0, loc))
	yieldPerKWp := (end.YieldTotalKWh - start.YieldTotalKWh) / (plant.DefaultPeakPowerW / 1000)
	if yieldPerKWp < 850 || yieldPerKWp > 1250 {
		t.Errorf("annual yield %.0f kWh/kWp, want 850..1250", yieldPerKWp)
	}
	// Load = PV - export + import, so the counters give the year's consumption.
	load := (end.YieldTotalKWh - start.YieldTotalKWh) - (end.ExportedTotalKWh - start.ExportedTotalKWh) + (end.ImportedTotalKWh - start.ImportedTotalKWh)
	if load < 4050 || load > 4950 {
		t.Errorf("annual load %.0f kWh, want 4,500 ± 10 %%", load)
	}
}

func TestClockChangeDaysKeepTheirHoursAndResetAtMidnight(t *testing.T) {
	loc := zone(t)
	p := newPlant(loc)
	for _, c := range []struct {
		day     time.Time
		minutes int
	}{
		{time.Date(2026, 3, 29, 0, 0, 0, 0, loc), 23 * 60},
		{time.Date(2026, 10, 25, 0, 0, 0, 0, loc), 25 * 60},
	} {
		next := time.Date(c.day.Year(), c.day.Month(), c.day.Day()+1, 0, 0, 0, 0, loc)
		var yield float64
		n := 0
		for at := c.day; at.Before(next); at = at.Add(time.Minute) {
			yield += p.At(at).PVPowerW / 60_000
			n++
		}
		if n != c.minutes {
			t.Fatalf("%v has %d minutes, want %d", c.day.Format("2006-01-02"), n, c.minutes)
		}
		lastMinute := p.At(next.Add(-time.Minute))
		dayYield := lastMinute.YieldTodayKWh + lastMinute.PVPowerW/60_000
		if math.Abs(dayYield-yield) > 1e-6 {
			t.Errorf("%v: daily yield %v kWh, its minutes add up to %v kWh", c.day.Format("2006-01-02"), dayYield, yield)
		}
		if got := p.At(next).YieldTodayKWh; got != 0 {
			t.Errorf("daily yield at midnight after %v = %v kWh, want 0", c.day.Format("2006-01-02"), got)
		}
	}
}

// America/Santiago starts DST at midnight: 2025-09-07 begins at 01:00 and has
// 23 hours. The day must keep its date and its yield must start from 0.
func TestADayWithoutMidnightStartsAtTheEndOfTheGap(t *testing.T) {
	loc, err := time.LoadLocation("America/Santiago")
	if err != nil {
		t.Fatal(err)
	}
	start := plant.StartOfDay(time.Date(2025, 9, 7, 12, 0, 0, 0, loc))
	if start.Day() != 7 || start.Hour() != 1 {
		t.Fatalf("the 7th starts at %s, want 01:00 on the 7th", start.Format(time.DateTime))
	}
	if next := plant.NextDay(start); next.Sub(start) != 23*time.Hour {
		t.Errorf("the 7th lasts %s, want 23h", next.Sub(start))
	}
	p := newPlant(loc)
	if s := p.At(start); s.YieldTodayKWh != 0 {
		t.Errorf("yield at the start of the 7th = %v, want 0", s.YieldTodayKWh)
	}
	evening := p.At(time.Date(2025, 9, 7, 20, 0, 0, 0, loc))
	if evening.YieldTodayKWh <= 0 {
		t.Errorf("no yield on the 7th by 20:00")
	}
}
