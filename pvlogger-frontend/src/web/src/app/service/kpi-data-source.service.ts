import { DOCUMENT, Inject, Injectable, OnDestroy } from '@angular/core';
import { fromEvent, Subscription } from 'rxjs';
import { AppBus } from './app-bus.service';
import { KpiFetchServiceService } from './kpi-fetch-service.service';

@Injectable({
  providedIn: 'root'
})
export class KpiDataSourceService implements OnDestroy {
  /** A tab hidden for less than this is not resynced, so quick tab flicks cost nothing. */
  public static readonly RESYNC_AFTER_HIDDEN_MS = 60_000;
  private dateSubscription: Subscription;
  private visibilitySubscription: Subscription;
  private shownDate = new Date();
  /** Whether the dashboard shows the current day, as opposed to a past day the user picked. */
  private followsToday = true;
  private hiddenSince: number | undefined;

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

  public getImportedPowerData(): number[][] {
    return this.kpiFetchService.importedpowerdata.slice();
  }

  public getExportedPowerData(): number[][] {
    return this.kpiFetchService.exportedpowerdata.slice();
  }

  public getSelfConsumedPowerData(): number[][] {
    return this.kpiFetchService.selfconsumedpowerdata.slice();
  }

  public getUsedPowerData(): number[][] {
    return this.kpiFetchService.usedpowerdata.slice();
  }

  constructor(private kpiFetchService: KpiFetchServiceService, private appBus: AppBus,
              @Inject(DOCUMENT) private document: Document) {
    this.dateSubscription = this.appBus.dateChanged.subscribe((date) => {
      this.shownDate = date;
      this.followsToday = this.isToday(date);
      this.kpiFetchService.detachFromLiveData();
      this.kpiFetchService.clearData();
      this.kpiFetchService.fetchDataFor(date);
      if (this.followsToday) {
        this.kpiFetchService.attachToLiveData();
      }
    });
    this.visibilitySubscription = fromEvent(this.document, 'visibilitychange')
      .subscribe(() => this.onVisibilityChange());
    this.kpiFetchService.fetchDataFor(this.shownDate);
    this.kpiFetchService.attachToLiveData();
  }

  ngOnDestroy(): void {
    this.dateSubscription.unsubscribe();
    this.visibilitySubscription.unsubscribe();
  }

  /**
   * A background tab gets paused (sleep, browser tab freezing, network change):
   * the live socket may have missed minutes, died without noticing, or the day
   * may have changed. When the tab comes back, catch up (backlog item 46).
   */
  private onVisibilityChange() {
    if (this.document.visibilityState === 'hidden') {
      this.hiddenSince ??= Date.now();
      return;
    }
    const hiddenFor = this.hiddenSince === undefined ? 0 : Date.now() - this.hiddenSince;
    this.hiddenSince = undefined;
    if (!this.followsToday || hiddenFor < KpiDataSourceService.RESYNC_AFTER_HIDDEN_MS) {
      return;
    }
    if (!this.isToday(this.shownDate)) {
      // Missed midnight: move on to today, as if the user had picked it.
      this.appBus.publishDateChanged(new Date());
      return;
    }
    this.kpiFetchService.refreshDataFor(this.shownDate);
    this.kpiFetchService.attachToLiveData();
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
