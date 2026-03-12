import { KpiValues, emptyKpiValues } from './kpi-values.model';

describe('KpiValues', () => {
  it('emptyKpiValues should return all zero values', () => {
    const kpi = emptyKpiValues();
    expect(kpi.createdAt).toBe(0);
    expect(kpi.solarPower).toBe(0);
    expect(kpi.gridPower).toBe(0);
    expect(kpi.solarToHousePower).toBe(0);
    expect(kpi.gridToHousePower).toBe(0);
    expect(kpi.solarToGridPower).toBe(0);
    expect(kpi.dailyEnergyYield).toBe(0);
    expect(kpi.ownConsumption).toBe(0);
    expect(kpi.dailyEnergySelfCovered).toBe(0);
    expect(kpi.dailyEnergyExported).toBe(0);
    expect(kpi.dailyEnergyImported).toBe(0);
    expect(kpi.dailyEnergyConsumed).toBe(0);
  });

  it('emptyKpiValues should return independent instances', () => {
    const a = emptyKpiValues();
    const b = emptyKpiValues();
    a.solarPower = 42;
    expect(b.solarPower).toBe(0);
  });
});
