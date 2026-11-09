import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHighcharts } from 'highcharts-angular';

import { ImportExportChartComponent } from './import-export-chart.component';

describe('ImportExportChartComponent', () => {
  let component: ImportExportChartComponent;
  let fixture: ComponentFixture<ImportExportChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ImportExportChartComponent],
    providers: [provideHttpClient(withXhr()), provideHttpClientTesting(), ...provideHighcharts()]
})
    .compileComponents();

    fixture = TestBed.createComponent(ImportExportChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
