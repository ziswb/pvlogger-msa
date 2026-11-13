import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withXhr } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { KpiDataSourceService } from './kpi-data-source.service';
import { KpiFetchServiceService } from './kpi-fetch-service.service';
import { AppBus } from './app-bus.service';

describe('KpiDataSourceService', () => {
  let service: KpiDataSourceService;
  let fetchService: KpiFetchServiceService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withXhr()), provideHttpClientTesting()]
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

  it('should return a copy of importedPowerData', () => {
    fetchService.importedpowerdata = [[10, 20]];
    const result = service.getImportedPowerData();
    expect(result).toEqual([[10, 20]]);
    expect(result).not.toBe(fetchService.importedpowerdata);
  });

  it('should return a copy of exportedPowerData', () => {
    fetchService.exportedpowerdata = [[10, -20]];
    const result = service.getExportedPowerData();
    expect(result).toEqual([[10, -20]]);
    expect(result).not.toBe(fetchService.exportedpowerdata);
  });

  it('should return a copy of selfConsumedPowerData', () => {
    fetchService.selfconsumedpowerdata = [[10, 30]];
    const result = service.getSelfConsumedPowerData();
    expect(result).toEqual([[10, 30]]);
    expect(result).not.toBe(fetchService.selfconsumedpowerdata);
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

  describe('when the tab comes back to the foreground', () => {
    const hiddenLongEnough = KpiDataSourceService.RESYNC_AFTER_HIDDEN_MS;
    let visibility: DocumentVisibilityState;
    let fetchDataFor: ReturnType<typeof vi.spyOn>;
    let refreshDataFor: ReturnType<typeof vi.spyOn>;
    let attachToLiveData: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
      vi.useFakeTimers({ toFake: ['Date'] });
      vi.setSystemTime(new Date(2026, 10, 11, 14, 0));
      visibility = 'visible';
      Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => visibility });
      // A fresh service, with the fetch service's network calls stubbed.
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        providers: [provideHttpClient(withXhr()), provideHttpClientTesting()]
      });
      fetchService = TestBed.inject(KpiFetchServiceService);
      fetchDataFor = vi.spyOn(fetchService, 'fetchDataFor').mockImplementation(() => {});
      refreshDataFor = vi.spyOn(fetchService, 'refreshDataFor').mockImplementation(() => {});
      attachToLiveData = vi.spyOn(fetchService, 'attachToLiveData').mockImplementation(() => {});
      vi.spyOn(fetchService, 'detachFromLiveData').mockImplementation(() => {});
      service = TestBed.inject(KpiDataSourceService);
      fetchDataFor.mockClear();
      attachToLiveData.mockClear();
    });

    afterEach(() => {
      service.ngOnDestroy();
      delete (document as any).visibilityState;
      vi.useRealTimers();
    });

    function hideFor(ms: number) {
      visibility = 'hidden';
      document.dispatchEvent(new Event('visibilitychange'));
      vi.setSystemTime(Date.now() + ms);
      visibility = 'visible';
      document.dispatchEvent(new Event('visibilitychange'));
    }

    it('should merge the day again and reconnect the live data', () => {
      hideFor(hiddenLongEnough);

      expect(refreshDataFor).toHaveBeenCalledTimes(1);
      expect(refreshDataFor.mock.calls[0][0].getDate()).toBe(11);
      expect(attachToLiveData).toHaveBeenCalledTimes(1);
      expect(fetchDataFor).not.toHaveBeenCalled(); // no clearing, so no empty flash
    });

    it('should not resync after a short look at another tab', () => {
      hideFor(hiddenLongEnough - 1);

      expect(refreshDataFor).not.toHaveBeenCalled();
      expect(attachToLiveData).not.toHaveBeenCalled();
    });

    it('should move on to today when midnight passed while hidden', () => {
      const changes: Date[] = [];
      TestBed.inject(AppBus).dateChanged.subscribe((date) => changes.push(date));

      hideFor(12 * 60 * 60 * 1000); // 14:00 on the 11th to 02:00 on the 12th

      expect(changes.map((date) => date.getDate())).toEqual([12]);
      expect(fetchDataFor).toHaveBeenCalledTimes(1); // the fresh day, through the datepicker path
      expect(refreshDataFor).not.toHaveBeenCalled();
    });

    it('should leave a past day the user picked alone', () => {
      TestBed.inject(AppBus).publishDateChanged(new Date(2026, 10, 1));
      fetchDataFor.mockClear();

      hideFor(hiddenLongEnough);

      expect(refreshDataFor).not.toHaveBeenCalled();
      expect(fetchDataFor).not.toHaveBeenCalled();
      expect(attachToLiveData).not.toHaveBeenCalled();
    });

    it('should stop listening once destroyed', () => {
      service.ngOnDestroy();

      hideFor(hiddenLongEnough);

      expect(refreshDataFor).not.toHaveBeenCalled();
    });
  });
});
