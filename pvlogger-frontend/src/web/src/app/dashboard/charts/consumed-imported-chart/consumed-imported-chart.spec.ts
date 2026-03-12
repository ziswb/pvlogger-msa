import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHighcharts } from 'highcharts-angular';

import { ConsumedImportedChart } from './consumed-imported-chart';

describe('ConsumedImportedChart', () => {
  let component: ConsumedImportedChart;
  let fixture: ComponentFixture<ConsumedImportedChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsumedImportedChart],
      providers: [provideHttpClient(), provideHttpClientTesting(), ...provideHighcharts()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConsumedImportedChart);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
