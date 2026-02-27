import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ImportExportChartComponent } from './import-export-chart.component';

describe('ImportExportChartComponent', () => {
  let component: ImportExportChartComponent;
  let fixture: ComponentFixture<ImportExportChartComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
    imports: [ImportExportChartComponent]
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
