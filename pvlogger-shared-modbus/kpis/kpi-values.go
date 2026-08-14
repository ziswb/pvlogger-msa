package kpis

type KpiLiveData struct {
	InverterActivePower *InverterActivePower
	MeterActivePower    *MeterActivePower
	// ConsumingPower is optional: set when the collector read the dongle's
	// coherent aggregate block (load register 37500, backlog 33). When nil,
	// Consuming falls back to the formula over inverter + meter power.
	ConsumingPower *ConsumingPower
}

func (k *KpiLiveData) Producing() W {
	return k.InverterActivePower.Producing()
}

func (k *KpiLiveData) Exporting() W {
	return k.MeterActivePower.Exporting()
}

func (k *KpiLiveData) Importing() W {
	return k.MeterActivePower.Importing()
}

func (k *KpiLiveData) Consuming() W {
	// Prefer the directly measured house load: it comes from the same dongle
	// snapshot as Producing/Exporting/Importing, so it cannot go negative
	// under fast irradiance changes the way the formula over separately
	// sampled registers could (backlog 33).
	if k.ConsumingPower != nil {
		return k.ConsumingPower.Consuming()
	}
	return k.Producing().Minus(k.Exporting()).Plus(k.Importing())
}

func (k *KpiLiveData) SelfConsuming() W {
	return k.Producing().Minus(k.Exporting())
}

type KpiTodayData struct {
	InverterTodayEnergy *InverterEnergyToday
	MeterTodayEnergy    *MeterEnergyToday
}

func (k *KpiTodayData) Produced() KWh {
	return k.InverterTodayEnergy.Produced()
}

func (k *KpiTodayData) Exported() KWh {
	return k.MeterTodayEnergy.Exported()
}

func (k *KpiTodayData) Imported() KWh {
	return k.MeterTodayEnergy.Imported()
}

func (k *KpiTodayData) Consumed() KWh {
	return k.Produced().Minus(k.Exported()).Plus(k.Imported())
}

func (k *KpiTodayData) SelfConsumed() KWh {
	return k.Produced().Minus(k.Exported())
}
