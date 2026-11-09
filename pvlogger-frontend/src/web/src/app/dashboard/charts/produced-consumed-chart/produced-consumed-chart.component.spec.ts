import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';

import { ProducedConsumedChartComponent } from './produced-consumed-chart.component';
import { expectChartRendered, provideTestHighcharts } from '../testing';

describe('ProducedConsumedChartComponent', () => {
  let component: ProducedConsumedChartComponent;
  let fixture: ComponentFixture<ProducedConsumedChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ProducedConsumedChartComponent],
    providers: [provideHttpClient(withXhr()), provideHttpClientTesting(), provideTestHighcharts()]
})
    .compileComponents();

    fixture = TestBed.createComponent(ProducedConsumedChartComponent);
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
