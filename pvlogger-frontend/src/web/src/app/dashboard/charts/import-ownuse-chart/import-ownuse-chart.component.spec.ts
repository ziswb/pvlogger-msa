import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHighcharts } from 'highcharts-angular';

import { ImportOwnuseChartComponent } from './import-ownuse-chart.component';

describe('ImportOwnuseChartComponent', () => {
  let component: ImportOwnuseChartComponent;
  let fixture: ComponentFixture<ImportOwnuseChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ImportOwnuseChartComponent],
    providers: [provideHttpClient(withXhr()), provideHttpClientTesting(), provideHighcharts()]
})
    .compileComponents();

    fixture = TestBed.createComponent(ImportOwnuseChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
