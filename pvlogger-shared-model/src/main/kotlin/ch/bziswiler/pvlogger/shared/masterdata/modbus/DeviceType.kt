package ch.bziswiler.pvlogger.shared.masterdata.modbus

// deviceTypeId values are Huawei FusionSolar/SmartPVMS device type ids
// (used by pvlogger-fusionsolar's API requests), not Modbus unit ids.
private const val METER_TYPE_ID = 47
private const val INVERTER_TYPE_ID = 1
private const val DONGLE_TYPE_ID = 62

// DONGLE is the SmartDongle's own logical Modbus device (unit 100, see
// DONGLE_DEVICE_ID): it serves the aggregate live-power block 37498 x20 the
// KPIs are computed from (ADR-0016, backlog items 33/35).
enum class DeviceType(val deviceTypeId: Int) { METER(METER_TYPE_ID), INVERTER(INVERTER_TYPE_ID), DONGLE(DONGLE_TYPE_ID) }
