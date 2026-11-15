package mock

import (
	"errors"
	"math"
	"net"
	"strings"
	"testing"
	"time"

	modbus "pvlogger-shared-modbus"
	"pvlogger-shared-modbus/model"
	"pvlogger-shared-modbus/registers"
	"pvlogger-shared-modbus/request"
	"pvlogger-shared-modbus/response"

	"pvlogger-modbus-mock/internal/plant"
)

const dongleUnit = 100

// A sunny summer afternoon with export, and a night with import.
var (
	exporting = time.Date(2025, 6, 21, 13, 0, 0, 0, time.UTC)
	importing = time.Date(2025, 6, 21, 23, 0, 0, 0, time.UTC)
)

// fixture runs a mock server whose clock stands at *now and returns a
// connected client of the kind the collectors use.
type fixture struct {
	t      *testing.T
	plant  *plant.Plant
	now    time.Time
	client *modbus.TcpHandler
	reqId  model.UShort
}

func newFixture(t *testing.T, now time.Time) *fixture {
	t.Helper()
	f := &fixture{t: t, plant: plant.New(plant.Config{Location: time.UTC}), now: now}
	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	srv := NewServer(f.plant, dongleUnit, func() time.Time { return f.now })
	go func() { _ = srv.Serve(ln) }()
	t.Cleanup(func() { _ = ln.Close() })
	f.client = modbus.NewTcpHandler(ln.Addr().String(), time.Second, 0)
	if err := f.client.Connect(); err != nil {
		t.Fatal(err)
	}
	t.Cleanup(func() { _ = f.client.Close() })
	return f
}

// read sends a read holding registers request as the collectors encode it and
// decodes the answer with the collectors' decoder.
func (f *fixture) read(unit model.UByte, defs []model.RegisterDefinition) map[int]any {
	f.t.Helper()
	f.reqId++
	req := &request.ReadHoldingRegistersValueRequest{
		RequestId:           model.NewRequestId(f.reqId),
		LogicDeviceId:       unit,
		RegisterDefinitions: defs,
	}
	header, pdu, err := f.client.ExecuteRequest(req.EncodeToBytes())
	if err != nil {
		f.t.Fatalf("request to unit %d: %v", unit, err)
	}
	if err := response.CheckRegisterValuesPayload(defs, pdu); err != nil {
		f.t.Fatalf("response from unit %d: %v", unit, err)
	}
	if header.ProtocolIdentifier != f.reqId || header.LogicDeviceId != unit {
		f.t.Fatalf("header %+v does not echo request %d to unit %d", header, f.reqId, unit)
	}
	resp := response.NewRegisterValueResponseFromBytes(defs, req.JobId, req.RequestId, req.RequestTime, *header, pdu)
	values := map[int]any{}
	for _, v := range resp.Values {
		values[v.Definition.RegisterAddress.Value] = v.Value
	}
	return values
}

func TestDongleBlockIsThePlantSnapshot(t *testing.T) {
	for name, now := range map[string]time.Time{"exporting": exporting, "importing": importing} {
		t.Run(name, func(t *testing.T) {
			f := newFixture(t, now)
			s := f.plant.At(now)
			v := f.read(dongleUnit, registers.DongleBlockRegisters())

			load := float64(v[37500].(model.UInt))
			grid := float64(v[37502].(int32)) // positive = import
			activeKW := float64(v[37516].(int32)) / 1000

			assertNear(t, "load power W", load, s.LoadPowerW, 1)
			assertNear(t, "grid power W", grid, s.GridImportW-s.GridExportW, 1)
			assertNear(t, "total active power kW", activeKW, s.PVPowerW/1000, 0.001)
			if v[37504].(int32) != 0 {
				t.Errorf("battery power = %d, want 0 (no battery)", v[37504])
			}
			// The live KPI identity the aggregation relies on holds exactly
			// on the raw words (37516 carries W as kW with gain 1000).
			if got := int64(v[37516].(int32)) + int64(v[37502].(int32)); got != int64(v[37500].(model.UInt)) {
				t.Errorf("active + grid = %d W, want load %d W", got, v[37500])
			}
		})
	}
}

func TestCountersAreThePlantCounters(t *testing.T) {
	f := newFixture(t, exporting)
	s := f.plant.At(exporting)
	const inverterUnit = 1

	daily := f.read(inverterUnit, registers.InverterDailyYieldRegisters())
	total := f.read(inverterUnit, registers.InverterAccumulatedYieldRegisters())
	meter := f.read(inverterUnit, registers.MeterEnergyRegisters())

	// Registers carry kWh with gain 100.
	kWh := func(v any) float64 { return float64(v.(model.UInt)) / 100 }
	assertNear(t, "daily yield", kWh(daily[32114]), s.YieldTodayKWh, 0.005)
	assertNear(t, "accumulated yield", kWh(total[32106]), s.YieldTotalKWh, 0.005)
	assertNear(t, "exported", kWh(meter[37119]), s.ExportedTotalKWh, 0.005)
	assertNear(t, "imported", kWh(meter[37121]), s.ImportedTotalKWh, 0.005)
}

// def is a catalog row as the masterdata server serves it.
func def(addr, quantity int, typ string) model.RegisterDefinition {
	return model.RegisterDefinition{
		No: addr, Type: typ,
		RegisterAddress: model.RegisterAddress{Value: addr},
		Quantity:        model.Quantity{Value: quantity},
	}
}

func TestInverterAndMeterFollowThePlant(t *testing.T) {
	for name, now := range map[string]time.Time{"exporting": exporting, "importing": importing} {
		t.Run(name, func(t *testing.T) {
			f := newFixture(t, now)
			s := f.plant.At(now)
			inverter := f.read(1, []model.RegisterDefinition{
				def(32064, 2, "I32"), // input power, kW gain 1000
				def(32066, 1, "U16"), def(32067, 1, "U16"), def(32068, 1, "U16"),
				def(32069, 1, "U16"), def(32070, 1, "U16"), def(32071, 1, "U16"),
				def(32072, 2, "I32"), def(32074, 2, "I32"), def(32076, 2, "I32"),
				def(32078, 2, "I32"),
				def(32080, 2, "I32"), // active power, kW gain 1000
			})
			status := f.read(1, []model.RegisterDefinition{def(32089, 1, "U16")})
			meter := f.read(1, []model.RegisterDefinition{def(37113, 2, "I32")})

			activeW := float64(inverter[32080].(int32))
			assertNear(t, "inverter active power W", activeW, s.PVPowerW, 1)
			if input := float64(inverter[32064].(int32)); input < activeW {
				t.Errorf("input power %v W below active power %v W", input, activeW)
			}
			assertNear(t, "meter active power W (positive = export)",
				float64(meter[37113].(int32)), s.GridExportW-s.GridImportW, 1)

			// 0x0200 on-grid while producing, 0xA000 standby without irradiation.
			want := model.UShort(0xA000)
			if s.PVPowerW > 0 {
				want = 0x0200
			}
			if status[32089] != want {
				t.Errorf("device status = 0x%04x, want 0x%04x", status[32089], want)
			}
		})
	}
}

func TestInverterIdentifiesAsAGenericModelWithAFakeSerial(t *testing.T) {
	f := newFixture(t, exporting)
	v := f.read(1, []model.RegisterDefinition{
		def(30000, 15, "STR"), def(30015, 10, "STR"), def(30025, 10, "STR"),
	})
	rated := f.read(1, []model.RegisterDefinition{def(30073, 2, "U32")})

	if v[30000] != "SUN2000-10KTL-M1" {
		t.Errorf("model = %q", v[30000])
	}
	if v[30015] != "SIM0000001" {
		t.Errorf("serial number = %q", v[30015])
	}
	if v[30025] == "" {
		t.Error("part number is empty")
	}
	if got := float64(rated[30073].(model.UInt)); got != plant.DefaultPeakPowerW {
		t.Errorf("rated power = %v W (kW gain 1000), want %v", got, plant.DefaultPeakPowerW)
	}
}

func TestRegistersTheMockDoesNotModelReadAsZero(t *testing.T) {
	f := newFixture(t, exporting)
	v := f.read(1, []model.RegisterDefinition{def(47000, 10, "MLD"), def(47010, 2, "U32")})
	if v[47010] != model.UInt(0) {
		t.Errorf("unmodelled U32 = %v, want 0", v[47010])
	}
}

// identify sends a read device identification request as the device collector
// does and returns the objects by ID.
func (f *fixture) identify(code, objectId model.UByte) map[model.UByte]string {
	f.t.Helper()
	f.reqId++
	req := &request.ReadDeviceIdentifiersRequest{
		RequestId: model.NewRequestId(f.reqId), LogicDeviceId: 1,
		ReadDevIdCode: code, ObjectId: objectId,
	}
	header, pdu, err := f.client.ExecuteRequest(req.EncodeToBytes())
	if err != nil {
		f.t.Fatalf("device identification 0x%02x/0x%02x: %v", code, objectId, err)
	}
	resp, err := response.NewReadDeviceIdentifiersResponseFromBytes(req.JobId, req.RequestId, req.RequestTime, *header, pdu)
	if err != nil {
		f.t.Fatalf("decoding device identification: %v", err)
	}
	if int(resp.NumberOfObjects) != len(resp.ObjectList) {
		f.t.Fatalf("%d objects announced, %d decoded", resp.NumberOfObjects, len(resp.ObjectList))
	}
	objects := map[model.UByte]string{}
	for _, o := range resp.ObjectList {
		objects[o.ObjectId] = string(o.ObjectValue)
	}
	return objects
}

func TestDeviceIdentifiersNameTheVendorAndProduct(t *testing.T) {
	f := newFixture(t, exporting)
	got := f.identify(0x01, 0x00)
	if got[0x00] != "HUAWEI" || got[0x01] != "SUN2000" || got[0x02] == "" {
		t.Errorf("basic identification = %q", got)
	}
}

func TestDeviceListHasTheInverter(t *testing.T) {
	f := newFixture(t, exporting)
	got := f.identify(0x03, 0x87)
	if got[0x87] != "\x01" {
		t.Errorf("number of devices = %q, want one", got[0x87])
	}
	for _, part := range []string{"1=" + "SUN2000-10KTL-M1", "4=SIM0000001", "5=1"} {
		if !strings.Contains(got[0x88], part) {
			t.Errorf("device description %q lacks %q", got[0x88], part)
		}
	}
}

func TestUnsupportedFunctionIsRefused(t *testing.T) {
	f := newFixture(t, exporting)
	// Write single register 40000 := 1.
	_, _, err := f.client.ExecuteRequest([]byte{0, 1, 0, 0, 0, 6, 1, 0x06, 0x9C, 0x40, 0, 1})
	var ex *modbus.ExceptionError
	if !errors.As(err, &ex) || ex.ExceptionCode != 0x01 {
		t.Errorf("error = %v, want exception 0x01 (illegal function)", err)
	}
}

func assertNear(t *testing.T, what string, got, want, tolerance float64) {
	t.Helper()
	if math.Abs(got-want) > tolerance {
		t.Errorf("%s = %v, want %v ± %v", what, got, want, tolerance)
	}
}
