import { firstValueFrom } from 'rxjs';
import { AppBus } from './app-bus.service';
import { KpiValues, emptyKpiValues } from '../model/kpi-values.model';

describe('AppBus', () => {
  let appBus: AppBus;

  beforeEach(() => {
    appBus = new AppBus();
  });

  describe('kpiValues', () => {
    it('should emit published KPI values', async () => {
      const kpi = emptyKpiValues();
      kpi.solarPower = 5000;
      kpi.gridPower = -200;

      const next = firstValueFrom(appBus.kpiValues);
      appBus.publishKpiValues(kpi);
      const value = await next;

      expect(value.solarPower).toBe(5000);
      expect(value.gridPower).toBe(-200);
    });

    it('should replay the last value to late subscribers', async () => {
      const kpi = emptyKpiValues();
      kpi.solarPower = 3000;

      // Publish before subscribing
      appBus.publishKpiValues(kpi);

      // Late subscriber should still get the value
      expect((await firstValueFrom(appBus.kpiValues)).solarPower).toBe(3000);
    });

    it('should deliver only the latest of the values published within one frame', async () => {
      // Regression: each queued live value used to redraw all charts at once,
      // which froze a tab the browser had paused in the background.
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
      await nextFrame();

      expect(received).toEqual([300]);
    });

    it('should replay only the last value (buffer size 1)', async () => {
      const kpi1 = emptyKpiValues();
      kpi1.solarPower = 100;
      const kpi2 = emptyKpiValues();
      kpi2.solarPower = 200;

      appBus.publishKpiValues(kpi1);
      appBus.publishKpiValues(kpi2);

      // Late subscriber should only get kpi2
      expect((await firstValueFrom(appBus.kpiValues)).solarPower).toBe(200);
    });
  });

  describe('dateChanged', () => {
    it('should emit published date', async () => {
      const date = new Date(2024, 5, 15);

      const next = firstValueFrom(appBus.dateChanged);
      appBus.publishDateChanged(date);
      const value = await next;

      expect(value.getFullYear()).toBe(2024);
      expect(value.getMonth()).toBe(5);
      expect(value.getDate()).toBe(15);
    });

    it('should replay the last date to late subscribers', async () => {
      const date = new Date(2024, 0, 1);

      appBus.publishDateChanged(date);

      const value = await firstValueFrom(appBus.dateChanged);
      expect(value.getFullYear()).toBe(2024);
      expect(value.getMonth()).toBe(0);
    });
  });
});

function nextFrame(): Promise<void> {
  return new Promise((resolve) => requestAnimationFrame(() => resolve()));
}
