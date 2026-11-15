package persistence

import (
	"os"
	"reflect"
	"regexp"
	"slices"
	"strings"
	"testing"
)

// The Kotlin class is what the data accessor deserialises. Every field there
// has a default, so a field the Go side leaves out silently reads as 0, and a
// name only Go knows is refused (400) at runtime. This catches both in the
// build instead (backlog 58).
const rawKpiKt = "../../pvlogger-modbus-data-accessor/src/main/kotlin/ch/bziswiler/pvlogger/dataaccessor/database/access/RawKpiRegisterValues.kt"

var kotlinValPattern = regexp.MustCompile(`(?m)^\s*val (\w+)\s*:`)

func TestRawKpiRowMatchesTheKotlinClass(t *testing.T) {
	content, err := os.ReadFile(rawKpiKt)
	if err != nil {
		t.Fatalf("reading %s: %v", rawKpiKt, err)
	}
	var kotlin []string
	for _, m := range kotlinValPattern.FindAllStringSubmatch(string(content), -1) {
		kotlin = append(kotlin, m[1])
	}
	if len(kotlin) == 0 {
		t.Fatalf("no `val name:` found in %s: parser broken or file moved", rawKpiKt)
	}

	var goNames []string
	typ := reflect.TypeFor[RawKpiRegisterValues]()
	for i := range typ.NumField() {
		goNames = append(goNames, strings.Split(typ.Field(i).Tag.Get("json"), ",")[0])
	}

	slices.Sort(kotlin)
	slices.Sort(goNames)
	if !slices.Equal(kotlin, goNames) {
		t.Errorf("JSON names differ:\n  Kotlin: %v\n  Go:     %v", kotlin, goNames)
	}
}
