import { Injectable, OnDestroy } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppBus } from './app-bus.service';
import { KpiFetchServiceService } from './kpi-fetch-service.service';

@Injectable({
  providedIn: 'root'
})
export class KpiDataSourceService implements OnDestroy {
  private dateSubscription: Subscription;

  public getDailyOwnusedData(): number[][] {
    return this.kpiFetchService.dailyownuseddata.slice();
  }

  public getDailyConsumedData(): number[][] {
    return this.kpiFetchService.dailyconsumeddata.slice();
  }

  public getDailyProducedData(): number[][] {
    return this.kpiFetchService.dailyproduceddata.slice();
  }

  public getDailyExportedData(): number[][] {
    return this.kpiFetchService.dailyexporteddata.slice();
  }

  public getDailyImportedData(): number[][] {
    return this.kpiFetchService.dailyimporteddata.slice();
  }

  public getSolarPowerData(): number[][] {
    return this.kpiFetchService.solarpowerdata.slice();
  }

  public getGridPowerData(): number[][] {
    return this.kpiFetchService.gridpowerdata.slice();
  }

  public getUsedPowerData(): number[][] {
    return this.kpiFetchService.usedpowerdata.slice();
  }

  constructor(private kpiFetchService: KpiFetchServiceService, private appBus: AppBus) {
    this.dateSubscription = this.appBus.dateChanged.subscribe((date) => {
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

  ngOnDestroy(): void {
    this.dateSubscription.unsubscribe();
  }

  private isToday(date: Date): boolean {
    const today = new Date();
    if (date.getDate() !== today.getDate()) {
      return false;
    }
    if (date.getMonth() !== today.getMonth()) {
      return false;
    }
    return date.getFullYear() === today.getFullYear();
  }
}
