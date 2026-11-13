import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ImportOwnuseChartComponent } from './import-ownuse-chart.component';
import { expectChartRendered, provideTestHighcharts, renderedSeriesInHiddenTab } from '../testing';

describe('ImportOwnuseChartComponent', () => {
  let component: ImportOwnuseChartComponent;
  let fixture: ComponentFixture<ImportOwnuseChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ImportOwnuseChartComponent],
    providers: [provideHttpClient(withXhr()), provideHttpClientTesting(), provideTestHighcharts()]
})
    .compileComponents();

    fixture = TestBed.createComponent(ImportOwnuseChartComponent);
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
    expect(await renderedSeriesInHiddenTab(ImportOwnuseChartComponent)).toEqual(['Imported Power', 'Self Consumed Power']);
  });
});
