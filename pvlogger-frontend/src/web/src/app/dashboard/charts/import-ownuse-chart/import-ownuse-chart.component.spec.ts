import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImportOwnuseChartComponent } from './import-ownuse-chart.component';

describe('ImportOwnuseChartComponent', () => {
  let component: ImportOwnuseChartComponent;
  let fixture: ComponentFixture<ImportOwnuseChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ImportOwnuseChartComponent ]
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
