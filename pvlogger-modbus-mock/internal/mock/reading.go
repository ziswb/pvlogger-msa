package mock

import (
	"math"

	"pvlogger-modbus-mock/internal/plant"
)

// Reading is what the dongle reports for a snapshot, at register resolution:
// whole watts, the grid as the difference of the rounded values (so active
// power + grid power = load holds exactly, as on the real dongle), counters to
// 0.01 kWh. The backfill writes the same values the live path reads.
type Reading struct {
	PVW, LoadW       float64
	GridImportW      float64 // negative: export
	YieldTotalKWh    float64
	YieldTodayKWh    float64
	ExportedTotalKWh float64
	ImportedTotalKWh float64
}

// ReadingOf returns the reading of the snapshot s.
func ReadingOf(s plant.Snapshot) Reading {
	pv := math.Round(s.PVPowerW)
	load := math.Round(s.LoadPowerW)
	return Reading{
		PVW:              pv,
		LoadW:            load,
		GridImportW:      load - pv,
		YieldTotalKWh:    hundredths(s.YieldTotalKWh),
		YieldTodayKWh:    hundredths(s.YieldTodayKWh),
		ExportedTotalKWh: hundredths(s.ExportedTotalKWh),
		ImportedTotalKWh: hundredths(s.ImportedTotalKWh),
	}
}

func hundredths(v float64) float64 { return math.Round(v*100) / 100 }
