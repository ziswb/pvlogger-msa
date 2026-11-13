import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { KpiChartComponent } from './kpi-chart.component';
import { expectChartRendered, provideTestHighcharts, renderedSeriesInHiddenTab } from '../testing';

describe('KpichartComponent', () => {
  let component: KpiChartComponent;
  let fixture: ComponentFixture<KpiChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [KpiChartComponent],
    providers: [provideHttpClient(withXhr()), provideHttpClientTesting(), provideTestHighcharts()]
})
    .compileComponents();

    fixture = TestBed.createComponent(KpiChartComponent);
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
    expect(await renderedSeriesInHiddenTab(KpiChartComponent)).toEqual(['Consumed', 'Produced', 'Imported', 'Exported', 'Self-Consumed']);
  });
});
