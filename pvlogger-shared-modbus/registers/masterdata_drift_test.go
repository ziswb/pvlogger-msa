package registers

import (
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"testing"

	"pvlogger-shared-modbus/model"
)

// The masterdata migration V2 catalogues the dongle aggregate block for the
// full collector's sweep; this package holds the same block for the fast
// collector and the shared KPI aggregation. The two must not drift: the
// aggregation reads 37500/37502/37516 back from the data accessor's
// per-TYPE value tables, so a type mismatch between what full's
// masterdata-driven sweep stores and what the aggregation queries would be a
// silent 404 (failed runs). This test parses the migration SQL and fails the
// Go build on divergence (same pattern as config/paths_drift_test.go).

const dongleMigrationSQL = "../../pvlogger-masterdata-server/src/main/resources/db/migration/V2__DongleAggregateBlock.sql"

// ('DONGLE', 2, 'Load power', 'RO', 'U32', 'W', 'ONE', 37500, 2, '', 'DYNAMIC')
var dongleRowPattern = regexp.MustCompile(
	`\('DONGLE',\s*\d+,\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*'([^']*)',\s*(\d+),\s*(\d+),\s*'[^']*',\s*'([^']*)'\)`)

type migrationRow struct {
	name, readWrite, regType, unit, gain, category string
	address, quantity                              int
}

func parseMigrationRows(t *testing.T) map[int]migrationRow {
	t.Helper()
	path, err := filepath.Abs(dongleMigrationSQL)
	if err != nil {
		t.Fatalf("failed to resolve %s: %v", dongleMigrationSQL, err)
	}
	content, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("failed to read %s: %v", path, err)
	}
	rows := make(map[int]migrationRow)
	for _, m := range dongleRowPattern.FindAllStringSubmatch(string(content), -1) {
		address, _ := strconv.Atoi(m[6])
		quantity, _ := strconv.Atoi(m[7])
		rows[address] = migrationRow{
			name: m[1], readWrite: m[2], regType: m[3], unit: m[4], gain: m[5],
			address: address, quantity: quantity, category: m[8],
		}
	}
	if len(rows) == 0 {
		t.Fatalf("no DONGLE rows parsed from %s — parser broken or migration moved", path)
	}
	return rows
}

func TestDongleBlockMatchesMasterdataMigration(t *testing.T) {
	migration := parseMigrationRows(t)
	definitions := DongleBlockRegisters()

	if len(migration) != len(definitions) {
		t.Errorf("migration has %d DONGLE rows, Go block has %d definitions", len(migration), len(definitions))
	}

	for _, def := range definitions {
		row, ok := migration[def.RegisterAddress.Value]
		if !ok {
			t.Errorf("register %d (%s) missing from the masterdata migration", def.RegisterAddress.Value, def.Name)
			continue
		}
		if row.name != def.Name {
			t.Errorf("register %d: name %q drifted from Go %q", def.RegisterAddress.Value, row.name, def.Name)
		}
		if row.regType != def.Type {
			t.Errorf("register %d: type %q drifted from Go %q — the aggregation's per-type read-back breaks", def.RegisterAddress.Value, row.regType, def.Type)
		}
		if row.quantity != def.Quantity.Value {
			t.Errorf("register %d: quantity %d drifted from Go %d", def.RegisterAddress.Value, row.quantity, def.Quantity.Value)
		}
		if row.readWrite != def.ReadWrite {
			t.Errorf("register %d: readWrite %q drifted from Go %q", def.RegisterAddress.Value, row.readWrite, def.ReadWrite)
		}
		if row.category != def.Category {
			t.Errorf("register %d: category %q drifted from Go %q — the full sweep reads DYNAMIC+ALARM only", def.RegisterAddress.Value, row.category, def.Category)
		}
		// Unit/gain: masterdata catalogues the device truth; only 37516
		// deviates in Go (kW/THOUSAND for the raw-row column convention,
		// documented in register-definitions.go). Everything else must match.
		if def.RegisterAddress.Value != DongleTotalActivePower.RegisterAddress.Value {
			if row.unit != def.Unit {
				t.Errorf("register %d: unit %q drifted from Go %q", def.RegisterAddress.Value, row.unit, def.Unit)
			}
			if row.gain != def.Gain {
				t.Errorf("register %d: gain %q drifted from Go %q", def.RegisterAddress.Value, row.gain, def.Gain)
			}
		}
	}
}

// TestDongleBlockIsOneConsecutiveRun pins the property the reserved filler
// exists for: sorted by address, every register starts where the previous one
// ends, so the collector's consecutive-register merge turns the whole block
// into a single Modbus request = one coherent snapshot (backlog 33).
func TestDongleBlockIsOneConsecutiveRun(t *testing.T) {
	assertConsecutive := func(t *testing.T, defs []model.RegisterDefinition, total int) {
		t.Helper()
		next := defs[0].RegisterAddress.Value
		registers := 0
		for _, def := range defs {
			if def.RegisterAddress.Value != next {
				t.Errorf("register %d does not start at %d — the block would split into multiple Modbus requests", def.RegisterAddress.Value, next)
			}
			next = def.RegisterAddress.Value + def.Quantity.Value
			registers += def.Quantity.Value
		}
		if registers != total {
			t.Errorf("block covers %d registers, want %d", registers, total)
		}
	}

	assertConsecutive(t, DongleBlockRegisters(), 20)

	// The migration must tile the same run: rebuild definitions from it.
	migration := parseMigrationRows(t)
	defs := make([]model.RegisterDefinition, 0, len(migration))
	for _, def := range DongleBlockRegisters() { // reuse the Go order (sorted by address)
		row := migration[def.RegisterAddress.Value]
		defs = append(defs, model.RegisterDefinition{
			RegisterAddress: model.RegisterAddress{Value: row.address},
			Quantity:        model.Quantity{Value: row.quantity},
		})
	}
	assertConsecutive(t, defs, 20)
}
