package kpi

type Values struct {
	CreatedAt              int64   `json:"createdAt"`
	SolarPower             float64 `json:"solarPower"`
	GridPower              float64 `json:"gridPower"`
	SolarToHousePower      float64 `json:"solarToHousePower"`
	GridToHousePower       float64 `json:"gridToHousePower"`
	SolarToGridPower       float64 `json:"solarToGridPower"`
	OwnConsumption         float64 `json:"ownConsumption"`
	DailyEnergyYield       float64 `json:"dailyEnergyYield"`
	DailyEnergySelfCovered float64 `json:"dailyEnergySelfCovered"`
	DailyEnergyExported    float64 `json:"dailyEnergyExported"`
	DailyEnergyImported    float64 `json:"dailyEnergyImported"`
	DailyEnergyConsumed    float64 `json:"dailyEnergyConsumed"`
}
