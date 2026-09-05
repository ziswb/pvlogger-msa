import { KpiFetchServiceService } from './kpi-fetch-service.service';
import { AppBus } from './app-bus.service';
import { KpiValues, emptyKpiValues } from '../model/kpi-values.model';

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

    it('should drop a repeated timestamp', () => {
      (service as any).pushData(createKpi(1000));
      (service as any).pushData(createKpi(1000)); // the same row again

      expect(service.solarpowerdata.length).toBe(2); // initial [[]] + 1 push
    });

    it('should still accept the day history when a live value arrived first', () => {
      // Regression: the live WebSocket delivers its first value immediately
      // (ADR-0019), so it normally beats the /v1/daydata response. A monotonic
      // high-water mark used to reject the whole day that arrived right after,
      // leaving the dashboard with a single live point.
      (service as any).pushData(createKpi(1800)); // live value for 'now'

      [600, 1200, 1800].forEach((createdAt) => (service as any).pushData(createKpi(createdAt)));

      // initial [[]] + the live point + the two history points not yet seen
      expect(service.solarpowerdata.length).toBe(4);
      expect(service.solarpowerdata.slice(1).map((entry) => entry[0])).toEqual([1800, 600, 1200]);
    });

    it('should accept data with increasing timestamps', () => {
      (service as any).pushData(createKpi(100));
      (service as any).pushData(createKpi(200));
      (service as any).pushData(createKpi(300));

      expect(service.solarpowerdata.length).toBe(4); // initial [[]] + 3 pushes
    });

    it('should negate dailyEnergyExported', () => {
      const kpi: KpiValues = { ...emptyKpiValues(), createdAt: 1000, dailyEnergyExported: 7.5 };

      (service as any).pushData(kpi);

      expect(service.dailyexporteddata[1]).toEqual([1000, -7.5]);
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

    it('should forget which timestamps were already seen', () => {
      (service as any).pushData(createKpi(500));
      service.clearData();
      (service as any).pushData(createKpi(500)); // same day, freshly loaded

      expect(service.solarpowerdata.length).toBe(2);
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
      (service as any).pushData(createKpi(50)); // a different day, older timestamps

      expect(service.solarpowerdata.length).toBe(2);
    });
  });

  describe('publishData', () => {
    it('should publish the value it is given', (done) => {
      appBus.kpiValues.subscribe((value) => {
        if (value.createdAt === 0) { return; } // the clearData reset
        expect(value.createdAt).toBe(300);
        done();
      });

      (service as any).publishData(createKpi(300, 4.2));
    });

    it('should not let a late history row move the current values backwards', () => {
      const published: KpiValues[] = [];
      appBus.kpiValues.subscribe((value) => published.push(value));

      (service as any).publishData(createKpi(1800, 4.2)); // live value for 'now'
      (service as any).publishData(createKpi(1200, 1.1)); // last row of the history, arriving after

      expect(published.length).toBe(2); // both still notify, so the charts redraw
      expect(published[1].createdAt).toBe(1800);
      expect(published[1].solarPower).toBe(4.2);
    });

    it('should follow a newer value', () => {
      const published: KpiValues[] = [];
      appBus.kpiValues.subscribe((value) => published.push(value));

      (service as any).publishData(createKpi(1200, 1.1));
      (service as any).publishData(createKpi(1800, 4.2));

      expect(published[published.length - 1].createdAt).toBe(1800);
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
  return {
    ...emptyKpiValues(),
    createdAt,
    solarPower,
    gridPower,
    ownConsumption,
    dailyEnergyExported,
    dailyEnergyImported,
    dailyEnergySelfCovered,
    dailyEnergyConsumed,
  };
}
