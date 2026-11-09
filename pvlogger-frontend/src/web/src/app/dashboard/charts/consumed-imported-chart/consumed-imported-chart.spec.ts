import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ConsumedImportedChart } from './consumed-imported-chart';
import { expectChartRendered, provideTestHighcharts } from '../testing';

describe('ConsumedImportedChart', () => {
  let component: ConsumedImportedChart;
  let fixture: ComponentFixture<ConsumedImportedChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConsumedImportedChart],
      providers: [provideHttpClient(withXhr()), provideHttpClientTesting(), provideTestHighcharts()]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConsumedImportedChart);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render a chart', async () => {
    await expectChartRendered(fixture);
  });
});
