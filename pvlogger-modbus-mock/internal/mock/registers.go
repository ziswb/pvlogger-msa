package mock

import (
	"math"

	"pvlogger-modbus-mock/internal/plant"
)

// table maps register addresses to 16-bit words. Addresses it does not hold
// read as 0, so a sweep over any catalog range gets a complete answer.
type table map[uint16]uint16

func (t table) u16(addr uint16, v uint16) { t[addr] = v }

func (t table) i16(addr uint16, v int16) { t[addr] = uint16(v) }

func (t table) u32(addr uint16, v uint32) {
	t[addr] = uint16(v >> 16)
	t[addr+1] = uint16(v)
}

func (t table) i32(addr uint16, v int32) { t.u32(addr, uint32(v)) }

// str writes v as a NUL-padded string of quantity registers.
func (t table) str(addr uint16, quantity int, v string) {
	b := make([]byte, 2*quantity)
	copy(b, v)
	for i := range quantity {
		t[addr+uint16(i)] = uint16(b[2*i])<<8 | uint16(b[2*i+1])
	}
}

// registers returns the register table of unit for the snapshot s.
func (s *Server) registers(unit uint8, snap plant.Snapshot) table {
	t := table{}
	pvW := math.Round(snap.PVPowerW)
	loadW := math.Round(snap.LoadPowerW)
	// Grid as the difference of the rounded values, so that
	// active power + grid power = load holds exactly, as on the real dongle.
	gridImportW := loadW - pvW
	if unit == s.dongleUnit {
		t.u32(37498, uint32(math.Round(dcPowerW(pvW))))
		t.u32(37500, uint32(loadW))
		t.i32(37502, int32(gridImportW))
		t.i32(37504, 0)
		t.i32(37516, int32(pvW)) // kW with gain 1000
		return t
	}
	identity(t, s.plant.PeakPowerW())
	inverter(t, pvW)
	t.u32(32106, centi(snap.YieldTotalKWh))
	t.u32(32114, centi(snap.YieldTodayKWh))
	meter(t, -gridImportW)
	t.u32(37119, centi(snap.ExportedTotalKWh))
	t.u32(37121, centi(snap.ImportedTotalKWh))
	return t
}

// Identity of the simulated inverter: a public model name of the right size
// and obviously fake serial and part numbers.
const (
	inverterModel  = "SUN2000-10KTL-M1"
	inverterSerial = "SIM0000001"
	inverterPart   = "SIM-PN-0001"
)

// identity fills the static registers the device collector reads nightly.
func identity(t table, peakPowerW float64) {
	t.str(30000, 15, inverterModel)
	t.str(30015, 10, inverterSerial)
	t.str(30025, 10, inverterPart)
	t.u32(30073, uint32(peakPowerW)) // rated power, kW gain 1000
}

// Fixed electrical values of a healthy three-phase grid connection.
const (
	phaseVoltageV = 230.0
	lineVoltageV  = 400.0
	gridFrequency = 50.0
	stringVoltage = 600.0 // DC string voltage while producing
)

// inverter fills the instantaneous inverter registers for an AC output of pvW.
func inverter(t table, pvW float64) {
	producing := pvW > 0
	dcW := dcPowerW(pvW)
	if producing {
		// Two strings sharing the input power.
		for _, addr := range []uint16{32016, 32018} {
			t.i16(addr, int16(stringVoltage*10))
			t.i16(addr+1, int16(math.Round(dcW/2/stringVoltage*100)))
		}
	}
	t.i32(32064, int32(math.Round(dcW)))
	for i := range uint16(3) {
		t.u16(32066+i, uint16(lineVoltageV*10))
		t.u16(32069+i, uint16(phaseVoltageV*10))
		t.i32(32072+2*i, int32(math.Round(pvW/3/phaseVoltageV*1000)))
	}
	t.i32(32080, int32(pvW))
	t.i16(32084, 1000) // power factor, gain 1000
	t.u16(32085, uint16(gridFrequency*100))
	t.u16(32087, uint16(math.Round((25+pvW/400)*10))) // internal temperature
	if producing {
		t.u16(32086, 9750)   // efficiency %, gain 100
		t.u16(32089, 0x0200) // on-grid
	} else {
		t.u16(32089, 0xA000) // standby: no irradiation
	}
}

// meter fills the instantaneous meter registers for a grid exchange of
// feedInW (positive = export, the meter's sign convention).
func meter(t table, feedInW float64) {
	t.u16(37100, 1) // normal
	for i := range uint16(3) {
		t.i32(37101+2*i, int32(phaseVoltageV*10))
		t.i32(37107+2*i, int32(math.Round(feedInW/3/phaseVoltageV*100)))
		t.i32(37126+2*i, int32(lineVoltageV*10))
		t.i32(37132+2*i, int32(math.Round(feedInW/3)))
	}
	t.i32(37113, int32(feedInW))
	t.i16(37117, 1000) // power factor, gain 1000
	t.i16(37118, int16(gridFrequency*100))
	t.u16(37125, 1) // three-phase
	t.u16(37138, 1) // model detection: matches
}

// centi is a counter value with gain 100.
func centi(v float64) uint32 { return uint32(math.Round(v * 100)) }

// dcPowerW is the DC input power behind an AC output of acW.
func dcPowerW(acW float64) float64 {
	const inverterEfficiency = 0.975
	return acW / inverterEfficiency
}
