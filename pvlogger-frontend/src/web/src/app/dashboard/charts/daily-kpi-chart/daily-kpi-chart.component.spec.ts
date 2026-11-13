import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { DailyKpiChartComponent } from './daily-kpi-chart.component';
import { expectChartRendered, provideTestHighcharts, renderedSeriesInHiddenTab } from '../testing';

describe('DailyKpiChartComponent', () => {
  let component: DailyKpiChartComponent;
  let fixture: ComponentFixture<DailyKpiChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DailyKpiChartComponent],
      providers: [provideHttpClient(withXhr()), provideHttpClientTesting(), provideTestHighcharts()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DailyKpiChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render a chart', async () => {
    await expectChartRendered(fixture);
  });

  it('has its series when created before the first KPI value (hidden tab)', async () => {
    expect(await renderedSeriesInHiddenTab(DailyKpiChartComponent))
      .toEqual(['Consumed', 'Produced', 'Imported', 'Exported', 'Self-Consumed']);
  });
});
