export class KpiValues {
  createdAt: number = 0;
  solarPower: KW = new KW(0);
  gridPower: KW = new KW(0);
  solarToHousePower: KW = new KW(0);
  gridToHousePower: KW = new KW(0);
  solarToGridPower: KW = new KW(0);
  dailyEnergyYield: KWh = new KWh(0);
  ownConsumption: KW = new KW(0);
  dailyEnergySelfCovered: KWh = new KWh(0);
  dailyEnergyExported: KWh = new KWh(0);
  dailyEnergyImported: KWh = new KWh(0);
  dailyEnergyConsumed: KWh = new KWh(0);
}

export class KW {
  readonly kw: number;

  constructor(kw: number) {
    this.kw = kw;
  }
}

export class KWh {
  readonly kwh: number;

  constructor(kwh: number) {
    this.kwh = kwh;
  }
}
