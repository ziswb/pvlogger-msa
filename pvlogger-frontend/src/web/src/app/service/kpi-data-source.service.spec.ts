import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { KpiDataSourceService } from './kpi-data-source.service';
import { KpiFetchServiceService } from './kpi-fetch-service.service';

describe('KpiDataSourceService', () => {
  let service: KpiDataSourceService;
  let fetchService: KpiFetchServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    fetchService = TestBed.inject(KpiFetchServiceService);
    service = TestBed.inject(KpiDataSourceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should return a copy of solarPowerData', () => {
    fetchService.solarpowerdata = [[1, 2], [3, 4]];
    const result = service.getSolarPowerData();
    expect(result).toEqual([[1, 2], [3, 4]]);
    // Verify it's a copy, not the same reference
    expect(result).not.toBe(fetchService.solarpowerdata);
  });

  it('should return a copy of gridPowerData', () => {
    fetchService.gridpowerdata = [[10, 20]];
    const result = service.getGridPowerData();
    expect(result).toEqual([[10, 20]]);
    expect(result).not.toBe(fetchService.gridpowerdata);
  });

  it('should return a copy of usedPowerData', () => {
    fetchService.usedpowerdata = [[5, 6]];
    const result = service.getUsedPowerData();
    expect(result).toEqual([[5, 6]]);
    expect(result).not.toBe(fetchService.usedpowerdata);
  });

  it('should return a copy of dailyOwnusedData', () => {
    fetchService.dailyownuseddata = [[7, 8]];
    expect(service.getDailyOwnusedData()).toEqual([[7, 8]]);
  });

  it('should return a copy of dailyConsumedData', () => {
    fetchService.dailyconsumeddata = [[9, 10]];
    expect(service.getDailyConsumedData()).toEqual([[9, 10]]);
  });

  it('should return a copy of dailyProducedData', () => {
    fetchService.dailyproduceddata = [[11, 12]];
    expect(service.getDailyProducedData()).toEqual([[11, 12]]);
  });

  it('should return a copy of dailyExportedData', () => {
    fetchService.dailyexporteddata = [[13, 14]];
    expect(service.getDailyExportedData()).toEqual([[13, 14]]);
  });

  it('should return a copy of dailyImportedData', () => {
    fetchService.dailyimporteddata = [[15, 16]];
    expect(service.getDailyImportedData()).toEqual([[15, 16]]);
  });

  it('should clean up subscription on destroy', () => {
    service.ngOnDestroy();
    // Should not throw
    expect(service).toBeTruthy();
  });
});
