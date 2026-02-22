package kpis

type KpiLiveData struct {
	InverterActivePower *InverterActivePower
	MeterActivePower    *MeterActivePower
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
	return k.Producing().minus(k.Exporting()).plus(k.Importing())
}

func (k *KpiLiveData) SelfConsuming() W {
	return k.Producing().minus(k.Exporting())
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
	return k.Produced().minus(k.Exported()).plus(k.Imported())
}

func (k *KpiTodayData) SelfConsumed() KWh {
	return k.Produced().minus(k.Exported())
}
