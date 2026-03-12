export interface KpiValues {
  createdAt: number;
  solarPower: number;
  gridPower: number;
  solarToHousePower: number;
  gridToHousePower: number;
  solarToGridPower: number;
  dailyEnergyYield: number;
  ownConsumption: number;
  dailyEnergySelfCovered: number;
  dailyEnergyExported: number;
  dailyEnergyImported: number;
  dailyEnergyConsumed: number;
}

export function emptyKpiValues(): KpiValues {
  return {
    createdAt: 0,
    solarPower: 0,
    gridPower: 0,
    solarToHousePower: 0,
    gridToHousePower: 0,
    solarToGridPower: 0,
    dailyEnergyYield: 0,
    ownConsumption: 0,
    dailyEnergySelfCovered: 0,
    dailyEnergyExported: 0,
    dailyEnergyImported: 0,
    dailyEnergyConsumed: 0,
  };
}
