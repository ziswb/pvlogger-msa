import { ComponentFixture } from '@angular/core/testing';
import { provideHighcharts } from 'highcharts-angular';

/**
 * Highcharts for specs. highcharts-angular's default loader imports
 * 'highcharts/esm/highcharts' without a file extension, which Node's ESM loader
 * (Vitest runs specs in Node) refuses, and the chart then silently stays empty.
 */
export function provideTestHighcharts() {
  return provideHighcharts({ instance: () => import('highcharts').then((m) => m.default) });
}

/**
 * Resolves once the component rendered a Highcharts chart. Highcharts loads
 * asynchronously and the chart is created in an effect, which only runs on
 * change detection, so keep detecting changes while waiting.
 */
export async function expectChartRendered(fixture: ComponentFixture<unknown>): Promise<void> {
  await expect
    .poll(() => {
      fixture.detectChanges();
      return (fixture.nativeElement as HTMLElement).querySelector('.highcharts-container');
    })
    .toBeTruthy();
}
