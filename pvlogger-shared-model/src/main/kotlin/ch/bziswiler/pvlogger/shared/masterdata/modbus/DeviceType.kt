package ch.bziswiler.pvlogger.shared.masterdata.modbus

private const val METER_TYPE_ID = 47
private const val INVERTER_TYPE_ID = 1

enum class DeviceType(val deviceTypeId: Int) { METER(METER_TYPE_ID), INVERTER(INVERTER_TYPE_ID) }
