import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHighcharts } from 'highcharts-angular';

import { KpiChartComponent } from './kpi-chart.component';

describe('KpichartComponent', () => {
  let component: KpiChartComponent;
  let fixture: ComponentFixture<KpiChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [KpiChartComponent],
    providers: [provideHttpClient(), provideHttpClientTesting(), ...provideHighcharts()]
})
    .compileComponents();

    fixture = TestBed.createComponent(KpiChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
