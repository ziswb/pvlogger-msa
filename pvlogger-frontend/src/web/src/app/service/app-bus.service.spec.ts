import { AppBus } from './app-bus.service';
import { KpiValues, emptyKpiValues } from '../model/kpi-values.model';

describe('AppBus', () => {
  let appBus: AppBus;

  beforeEach(() => {
    appBus = new AppBus();
  });

  describe('kpiValues', () => {
    it('should emit published KPI values', (done) => {
      const kpi = emptyKpiValues();
      kpi.solarPower = 5000;
      kpi.gridPower = -200;

      appBus.kpiValues.subscribe((value) => {
        expect(value.solarPower).toBe(5000);
        expect(value.gridPower).toBe(-200);
        done();
      });

      appBus.publishKpiValues(kpi);
    });

    it('should replay the last value to late subscribers', (done) => {
      const kpi = emptyKpiValues();
      kpi.solarPower = 3000;

      // Publish before subscribing
      appBus.publishKpiValues(kpi);

      // Late subscriber should still get the value
      appBus.kpiValues.subscribe((value) => {
        expect(value.solarPower).toBe(3000);
        done();
      });
    });

    it('should emit multiple values in order', () => {
      const received: number[] = [];

      appBus.kpiValues.subscribe((value) => {
        received.push(value.solarPower);
      });

      const kpi1 = emptyKpiValues();
      kpi1.solarPower = 100;
      const kpi2 = emptyKpiValues();
      kpi2.solarPower = 200;
      const kpi3 = emptyKpiValues();
      kpi3.solarPower = 300;

      appBus.publishKpiValues(kpi1);
      appBus.publishKpiValues(kpi2);
      appBus.publishKpiValues(kpi3);

      expect(received).toEqual([100, 200, 300]);
    });

    it('should replay only the last value (buffer size 1)', (done) => {
      const kpi1 = emptyKpiValues();
      kpi1.solarPower = 100;
      const kpi2 = emptyKpiValues();
      kpi2.solarPower = 200;

      appBus.publishKpiValues(kpi1);
      appBus.publishKpiValues(kpi2);

      // Late subscriber should only get kpi2
      appBus.kpiValues.subscribe((value) => {
        expect(value.solarPower).toBe(200);
        done();
      });
    });
  });

  describe('dateChanged', () => {
    it('should emit published date', (done) => {
      const date = new Date(2024, 5, 15);

      appBus.dateChanged.subscribe((value) => {
        expect(value.getFullYear()).toBe(2024);
        expect(value.getMonth()).toBe(5);
        expect(value.getDate()).toBe(15);
        done();
      });

      appBus.publishDateChanged(date);
    });

    it('should replay the last date to late subscribers', (done) => {
      const date = new Date(2024, 0, 1);

      appBus.publishDateChanged(date);

      appBus.dateChanged.subscribe((value) => {
        expect(value.getFullYear()).toBe(2024);
        expect(value.getMonth()).toBe(0);
        done();
      });
    });
  });
});
