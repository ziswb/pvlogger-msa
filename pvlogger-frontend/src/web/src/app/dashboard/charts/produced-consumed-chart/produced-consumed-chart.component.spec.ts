import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProducedConsumedChartComponent } from './produced-consumed-chart.component';

describe('ProducedConsumedChartComponent', () => {
  let component: ProducedConsumedChartComponent;
  let fixture: ComponentFixture<ProducedConsumedChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ProducedConsumedChartComponent ]
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
