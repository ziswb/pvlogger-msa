import { Injectable } from '@angular/core';
import { AppBus } from './app-bus.service';
import { KpiFetchServiceService } from './kpi-fetch-service.service';

@Injectable({
  providedIn: 'root'
})
export class KpiDataSourceService {

  public getDailyOwnusedData(): Number[][] {
    return this.kpiFetchService.dailyownuseddata.slice();
  }

  public getDailyConsumedData(): Number[][] {
    return this.kpiFetchService.dailyconsumeddata.slice();
  }

  public getDailyProducedData(): Number[][] {
    return this.kpiFetchService.dailyproduceddata.slice();
  }

  public getDailyExportedData(): Number[][] {
    return this.kpiFetchService.dailyexporteddata.slice();
  }

  public getDailyImportedData(): Number[][] {
    return this.kpiFetchService.dailyimporteddata.slice();
  }

  public getSolarPowerData(): Number[][] {
    return this.kpiFetchService.solarpowerdata.slice();
  }

  public getGridPowerData(): Number[][] {
    return this.kpiFetchService.gridpowerdata.slice();
  }

  public getUsedPowerData(): Number[][] {
    return this.kpiFetchService.usedpowerdata.slice();
  }

  constructor(private kpiFetchService: KpiFetchServiceService, private appBus: AppBus) {
    console.log("KPI data source is alive");
    this.appBus.dateChanged.subscribe((date) => {
      console.log(`Date changed: ${date}`);
      this.kpiFetchService.detachFromLiveData();
      this.kpiFetchService.clearData();
      this.kpiFetchService.fetchDataFor(date);
      if (this.isToday(date)) {
        this.kpiFetchService.attachToLiveData();
      }
    });
    this.kpiFetchService.fetchDataFor(new Date());
    this.kpiFetchService.attachToLiveData();
  }

  private isToday(date: Date): boolean {
    let today = new Date();
    if (date.getDate() !== today.getDate()) {
      return false;
    }
    if (date.getMonth() !== today.getMonth()) {
      return false;
    }
    return date.getFullYear() === today.getFullYear();
  }
}
