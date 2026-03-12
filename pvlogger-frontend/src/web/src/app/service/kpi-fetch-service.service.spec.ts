import { KpiFetchServiceService } from './kpi-fetch-service.service';
import { AppBus } from './app-bus.service';
import { KpiValues } from '../model/kpi-values.model';

describe('KpiFetchServiceService', () => {
  let service: KpiFetchServiceService;
  let appBus: AppBus;

  beforeEach(() => {
    appBus = new AppBus();
    // Pass null as HttpClient — we only test pushData/clearData/sortData which don't use it
    service = new KpiFetchServiceService(null as any, appBus);
  });

  describe('pushData', () => {
    it('should populate all data arrays from a KPI value', () => {
      const kpi = createKpi(1000, 5.0, -200, 3.0, 10.5, 2.0, 8.5, 12.0);

      (service as any).pushData(kpi);

      // Each array starts with one empty entry [[]] plus the pushed entry
      expect(service.solarpowerdata.length).toBe(2);
      expect(service.solarpowerdata[1]).toEqual([1000, 5.0]);
      expect(service.gridpowerdata[1]).toEqual([1000, -200]);
      expect(service.usedpowerdata[1]).toEqual([1000, 3.0]);
      expect(service.dailyexporteddata[1]).toEqual([1000, -10.5]); // negated
      expect(service.dailyimporteddata[1]).toEqual([1000, 2.0]);
      expect(service.dailyownuseddata[1]).toEqual([1000, 8.5]);
      expect(service.dailyconsumeddata[1]).toEqual([1000, 12.0]);
      expect(service.dailyproduceddata[1]).toEqual([1000, 0]); // dailyEnergyYield defaults to 0
    });

    it('should drop data with timestamp <= last received', () => {
      const kpi1 = createKpi(1000);
      const kpi2 = createKpi(1000); // same timestamp
      const kpi3 = createKpi(500);  // older timestamp

      (service as any).pushData(kpi1);
      (service as any).pushData(kpi2);
      (service as any).pushData(kpi3);

      // Only the first one should have been pushed
      expect(service.solarpowerdata.length).toBe(2); // initial [[]] + 1 push
    });

    it('should accept data with increasing timestamps', () => {
      (service as any).pushData(createKpi(100));
      (service as any).pushData(createKpi(200));
      (service as any).pushData(createKpi(300));

      expect(service.solarpowerdata.length).toBe(4); // initial [[]] + 3 pushes
    });

    it('should negate dailyEnergyExported', () => {
      const kpi = new KpiValues();
      kpi.createdAt = 1000;
      kpi.dailyEnergyExported = 7.5;

      (service as any).pushData(kpi);

      expect(service.dailyexporteddata[1]).toEqual([1000, -7.5]);
    });

    it('should track maxCreatedAtReceived', () => {
      (service as any).pushData(createKpi(100));
      expect((service as any).maxCreatedAtReceived).toBe(100);

      (service as any).pushData(createKpi(500));
      expect((service as any).maxCreatedAtReceived).toBe(500);

      // Old data doesn't update the tracker
      (service as any).pushData(createKpi(200));
      expect((service as any).maxCreatedAtReceived).toBe(500);
    });
  });

  describe('clearData', () => {
    it('should reset all arrays to [[]]', () => {
      (service as any).pushData(createKpi(100));
      (service as any).pushData(createKpi(200));

      service.clearData();

      expect(service.solarpowerdata).toEqual([[]]);
      expect(service.gridpowerdata).toEqual([[]]);
      expect(service.usedpowerdata).toEqual([[]]);
      expect(service.dailyexporteddata).toEqual([[]]);
      expect(service.dailyimporteddata).toEqual([[]]);
      expect(service.dailyownuseddata).toEqual([[]]);
      expect(service.dailyconsumeddata).toEqual([[]]);
      expect(service.dailyproduceddata).toEqual([[]]);
    });

    it('should reset maxCreatedAtReceived to 0', () => {
      (service as any).pushData(createKpi(500));
      service.clearData();
      expect((service as any).maxCreatedAtReceived).toBe(0);
    });

    it('should publish empty KpiValues via AppBus', (done) => {
      appBus.kpiValues.subscribe((value) => {
        expect(value.solarPower).toBe(0);
        expect(value.createdAt).toBe(0);
        done();
      });

      service.clearData();
    });

    it('should allow new data after clearing', () => {
      (service as any).pushData(createKpi(100));
      service.clearData();
      (service as any).pushData(createKpi(50)); // would have been rejected before clear

      expect(service.solarpowerdata.length).toBe(2);
    });
  });

  describe('sortData', () => {
    it('should sort all arrays by timestamp (first element)', () => {
      (service as any).pushData(createKpi(300));
      (service as any).pushData(createKpi(400));

      // Manually insert unsorted entry to test sort
      service.solarpowerdata.splice(1, 0, [200, 99]);

      (service as any).sortData();

      // Should be sorted: [[]], [200, 99], [300, ...], [400, ...]
      expect(service.solarpowerdata[1][0]).toBe(200);
      expect(service.solarpowerdata[2][0]).toBe(300);
      expect(service.solarpowerdata[3][0]).toBe(400);
    });
  });
});

function createKpi(
  createdAt: number,
  solarPower = 0,
  gridPower = 0,
  ownConsumption = 0,
  dailyEnergyExported = 0,
  dailyEnergyImported = 0,
  dailyEnergySelfCovered = 0,
  dailyEnergyConsumed = 0
): KpiValues {
  const kpi = new KpiValues();
  kpi.createdAt = createdAt;
  kpi.solarPower = solarPower;
  kpi.gridPower = gridPower;
  kpi.ownConsumption = ownConsumption;
  kpi.dailyEnergyExported = dailyEnergyExported;
  kpi.dailyEnergyImported = dailyEnergyImported;
  kpi.dailyEnergySelfCovered = dailyEnergySelfCovered;
  kpi.dailyEnergyConsumed = dailyEnergyConsumed;
  return kpi;
}
