import {HttpClient} from '@angular/common/http';
import {Injectable, OnDestroy} from '@angular/core';
import {catchError, concatMap, EMPTY, of, retry, Subscription} from 'rxjs';
import {webSocket, WebSocketSubject} from 'rxjs/webSocket';
import {environment} from '../../environments/environment';
import {emptyKpiValues, KpiValues} from '../model/kpi-values.model';
import {AppBus} from './app-bus.service';
import {DateUtil} from './date.util';

@Injectable({
  providedIn: 'root'
})
export class KpiFetchServiceService implements OnDestroy {
  public static readonly SERVER_HOST = environment.serverHost;
  public static readonly SERVER_URL = environment.serverUrl;
  dailyownuseddata: number[][] = [[]];
  dailyconsumeddata: number[][] = [[]];
  dailyproduceddata: number[][] = [[]];
  dailyexporteddata: number[][] = [[]];
  dailyimporteddata: number[][] = [[]];
  solarpowerdata: number[][] = [[]];
  importedpowerdata: number[][] = [[]];
  exportedpowerdata: number[][] = [[]];
  selfconsumedpowerdata: number[][] = [[]];
  usedpowerdata: number[][] = [[]];
  private subject: WebSocketSubject<KpiValues> | undefined;
  private wsSubscription: Subscription | undefined;
  private fetchSubscription: Subscription | undefined;
  private seenCreatedAt = new Set<number>();
  private latest: KpiValues | undefined;
  private shownUntil = Number.MAX_SAFE_INTEGER;

  constructor(private http: HttpClient, private appBus: AppBus) {
  }

  ngOnDestroy(): void {
    this.detachFromLiveData();
    this.fetchSubscription?.unsubscribe();
  }

  public fetchDataFor(date: Date) {
    this.clearData();
    this.loadDay(date);
  }

  /**
   * Fetches the day again and merges it into what is shown, without clearing
   * first: points already shown are skipped (pushData), missing ones filled in.
   */
  public refreshDataFor(date: Date) {
    this.loadDay(date);
  }

  private loadDay(date: Date) {
    const requestedDate = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
    this.shownUntil = new DateUtil(date.getTime()).maxDate();
    this.fetchSubscription?.unsubscribe();
    this.fetchSubscription = this.http
      .get<KpiValues[]>(`${KpiFetchServiceService.SERVER_URL}/v1/daydata/${requestedDate}`)
      .pipe(
        retry({ count: 2, delay: 1000 }),
        catchError(() => EMPTY)
      )
      .subscribe((data: KpiValues[]) => {
        (data as KpiValues[]).sort((a, b) => this.compareNumbers(a.createdAt, b.createdAt)).forEach((value) => {
          this.pushData(value);
        });
        if (data && data.length && data.length > 0) {
          this.publishData(data[data.length - 1]);
        }
      });
  }

  public attachToLiveData() {
    this.detachFromLiveData();
    this.subject = webSocket<KpiValues>(`${environment.webSocketProtocol}://${KpiFetchServiceService.SERVER_HOST}/livedata`);
    this.wsSubscription = this.subject.pipe(
      concatMap(item => of(item)),
      retry({ delay: 3000 })
    ).subscribe({
      next: (data) => this.onLiveValue(data)
    });
  }

  private onLiveValue(data: KpiValues) {
    // A tab left open past midnight moves on to the new day, as if the user had
    // picked it. Otherwise it keeps every day's points, and redrawing them all on
    // each live value freezes the page longer every day.
    if (data.createdAt > this.shownUntil) {
      this.appBus.publishDateChanged(new Date(data.createdAt));
      return;
    }
    this.pushData(data);
    this.publishData(data);
  }

  private publishData(data: KpiValues) {
    this.sortData();
    // Always notify, so the charts redraw whichever source just delivered —
    // but never let an older row move the current-value tiles backwards. The
    // tail of the day's history can arrive after a newer live value.
    if (!this.latest || data.createdAt >= this.latest.createdAt) {
      this.latest = data;
    }
    this.appBus.publishKpiValues(this.latest);
  }

  public detachFromLiveData() {
    this.wsSubscription?.unsubscribe();
    this.wsSubscription = undefined;
    if (this.subject) {
      this.subject.complete();
      this.subject = undefined;
    }
  }

  public clearData() {
    this.seenCreatedAt.clear();
    this.latest = undefined;
    this.solarpowerdata = [[]];
    this.importedpowerdata = [[]];
    this.exportedpowerdata = [[]];
    this.selfconsumedpowerdata = [[]];
    this.usedpowerdata = [[]];
    this.dailyexporteddata = [[]];
    this.dailyimporteddata = [[]];
    this.dailyownuseddata = [[]];
    this.dailyconsumeddata = [[]];
    this.dailyproduceddata = [[]];
    this.appBus.publishKpiValues(emptyKpiValues());
  }

  private pushData(value: KpiValues) {
    // Deduplicate by timestamp, not by a monotonic high-water mark: the day's
    // history (GET /v1/daydata) and the live WebSocket are independent
    // requests, and since the socket delivers its first value immediately
    // (ADR-0019) it normally wins the race. A high-water mark would let that
    // one live value reject the entire day that arrives right after it.
    if (this.seenCreatedAt.has(value.createdAt)) {
      return;
    }
    this.seenCreatedAt.add(value.createdAt);
    this.solarpowerdata.push([value.createdAt, value.solarPower]);
    this.importedpowerdata.push([value.createdAt, value.gridToHousePower]);
    this.exportedpowerdata.push([value.createdAt, -value.solarToGridPower]);
    this.selfconsumedpowerdata.push([value.createdAt, value.solarToHousePower]);
    this.usedpowerdata.push([value.createdAt, value.ownConsumption]);
    this.dailyexporteddata.push([value.createdAt, -value.dailyEnergyExported]);
    this.dailyimporteddata.push([value.createdAt, value.dailyEnergyImported]);
    this.dailyownuseddata.push([value.createdAt, value.dailyEnergySelfCovered]);
    this.dailyconsumeddata.push([value.createdAt, value.dailyEnergyConsumed]);
    this.dailyproduceddata.push([value.createdAt, value.dailyEnergyYield]);
  }

  private compareArrays(a: number[], b: number[]): number {
    if (a[0] < b[0]) { return -1; }
    if (a[0] > b[0]) { return 1; }
    return 0;
  }

  private compareNumbers(a: number, b: number): number {
    if (a < b) { return -1; }
    if (a > b) { return 1; }
    return 0;
  }

  private sortData() {
    this.solarpowerdata.sort((a, b) => this.compareArrays(a, b));
    this.importedpowerdata.sort((a, b) => this.compareArrays(a, b));
    this.exportedpowerdata.sort((a, b) => this.compareArrays(a, b));
    this.selfconsumedpowerdata.sort((a, b) => this.compareArrays(a, b));
    this.usedpowerdata.sort((a, b) => this.compareArrays(a, b));
    this.dailyexporteddata.sort((a, b) => this.compareArrays(a, b));
    this.dailyimporteddata.sort((a, b) => this.compareArrays(a, b));
    this.dailyownuseddata.sort((a, b) => this.compareArrays(a, b));
    this.dailyconsumeddata.sort((a, b) => this.compareArrays(a, b));
    this.dailyproduceddata.sort((a, b) => this.compareArrays(a, b));
  }
}
