import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHighcharts } from 'highcharts-angular';

import { ProducedConsumedChartComponent } from './produced-consumed-chart.component';

describe('ProducedConsumedChartComponent', () => {
  let component: ProducedConsumedChartComponent;
  let fixture: ComponentFixture<ProducedConsumedChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ProducedConsumedChartComponent],
    providers: [provideHttpClient(), provideHttpClientTesting(), ...provideHighcharts()]
})
    .compileComponents();

    fixture = TestBed.createComponent(ProducedConsumedChartComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
