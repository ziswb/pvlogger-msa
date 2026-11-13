import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ImportExportChartComponent } from './import-export-chart.component';
import { expectChartRendered, provideTestHighcharts, renderedSeriesInHiddenTab } from '../testing';

describe('ImportExportChartComponent', () => {
  let component: ImportExportChartComponent;
  let fixture: ComponentFixture<ImportExportChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ImportExportChartComponent],
    providers: [provideHttpClient(withXhr()), provideHttpClientTesting(), provideTestHighcharts()]
})
    .compileComponents();

    fixture = TestBed.createComponent(ImportExportChartComponent);
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
    expect(await renderedSeriesInHiddenTab(ImportExportChartComponent)).toEqual(['Imported Power', 'Exported Power']);
  });
});
