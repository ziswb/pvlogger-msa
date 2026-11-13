import { Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHighcharts } from 'highcharts-angular';
import { NEVER } from 'rxjs';
import { AppBus } from '../../service/app-bus.service';

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

/**
 * Renders a chart the way a background tab does: AppBus delivers KPI values on
 * animation frames, and a hidden tab gets none, so Highcharts creates the chart
 * before any KPI value arrived. Highcharts' chart.update() never adds series to
 * an existing chart, so a chart created without its series stays empty for good
 * (backlog item 50). Resolves with the series names the chart's legend shows.
 */
export async function renderedSeriesInHiddenTab<T>(component: Type<T>): Promise<string[]> {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [component],
    providers: [
      provideHttpClient(withXhr()),
      provideHttpClientTesting(),
      provideTestHighcharts(),
      {
        provide: AppBus,
        useValue: { kpiValues: NEVER, dateChanged: NEVER, publishKpiValues: () => {}, publishDateChanged: () => {} },
      },
    ],
  });
  const fixture = TestBed.createComponent(component);
  fixture.detectChanges();
  await expectChartRendered(fixture);
  return [...(fixture.nativeElement as HTMLElement).querySelectorAll('.highcharts-legend-item text')]
    .map((text) => text.textContent ?? '');
}
