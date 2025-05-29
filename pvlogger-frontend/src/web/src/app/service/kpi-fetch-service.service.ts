import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { of } from 'rxjs';
import { concatMap } from 'rxjs/operators';
import { webSocket, WebSocketSubject } from 'rxjs/webSocket';
import { environment } from '../../environments/environment';
import { KpiValues } from '../model/kpi-values.model';
import { AppBus } from './app-bus.service';

@Injectable({
  providedIn: 'root'
})
export class KpiFetchServiceService {
  public static readonly SERVER_HOST = environment.serverHost;
  public static readonly SERVER_URL = environment.serverUrl;
  dailyownuseddata: Number[][] = [[]];
  dailyconsumeddata: Number[][] = [[]];
  dailyproduceddata: Number[][] = [[]];
  dailyexporteddata: Number[][] = [[]];
  dailyimporteddata: Number[][] = [[]];
  solarpowerdata: Number[][] = [[]];
  gridpowerdata: Number[][] = [[]];
  usedpowerdata: Number[][] = [[]];
  private subject: WebSocketSubject<KpiValues> | undefined;
  private maxCreatedAtReceived = 0;

  constructor(private http: HttpClient, private appBus: AppBus) {
    console.log("KPI fetch service alive");
  }

  public fetchDataFor(date: Date) {
    this.clearData();
    let requestedDate = `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}`;
    console.log(`fetch data query: '${KpiFetchServiceService.SERVER_URL}/v1/daydata/${requestedDate}'`);
    this.http.get<KpiValues[]>(`${KpiFetchServiceService.SERVER_URL}/v1/daydata/${requestedDate}`).subscribe((data: KpiValues[]) => {
      console.log(`data received`);
      console.log(data);
      (data as KpiValues[]).sort((a, b) => this.compareNumbers(a.createdAt, b.createdAt)).forEach((value) => {
        this.pushData(value);
      });
      if (data && data.length && data.length > 0) {
        this.publishData(data[data.length - 1]);
      }
    });
  }

  public attachToLiveData() {
    console.log("attaching to live data");
    this.subject = webSocket<KpiValues>(`${environment.webSocketProtocol}://${KpiFetchServiceService.SERVER_HOST}/livedata`);
    this.subject.pipe(concatMap(item => of(item).pipe())).subscribe(data => {
      console.log(`live data received`);
      console.log(data);
      this.pushData(data);
      this.publishData(data);
    });
  }

  private publishData(data: KpiValues) {
    this.sortData();
    console.log("publishing data");
    this.appBus.publishKpiValues(data);
  }

  public detachFromLiveData() {
    console.log("detaching from live data");
    if (this.subject) {
      console.log("unsubscribing from live data");
      this.subject.unsubscribe();
    }
  }

  public clearData() {
    console.log("clearing data");
    this.maxCreatedAtReceived = 0;
    this.solarpowerdata = [[]];
    this.gridpowerdata = [[]];
    this.usedpowerdata = [[]];
    this.dailyexporteddata = [[]];
    this.dailyimporteddata = [[]];
    this.dailyownuseddata = [[]];
    this.dailyconsumeddata = [[]];
    this.dailyproduceddata = [[]];
    this.appBus.publishKpiValues(new KpiValues());
  }

  private pushData(value: KpiValues) {
    if (value.createdAt <= this.maxCreatedAtReceived) {
      console.warn('Received old data - dropping it: "' + value.createdAt + '"');
      return;
    }
    this.maxCreatedAtReceived = value.createdAt;
    this.solarpowerdata.push([value.createdAt, Number(value.solarPower)]);
    this.gridpowerdata.push([value.createdAt, Number(value.gridPower)]);
    this.usedpowerdata.push([value.createdAt, Number(value.ownConsumption)]);
    this.dailyexporteddata.push([value.createdAt, -Number(value.dailyEnergyExported)]);
    this.dailyimporteddata.push([value.createdAt, Number(value.dailyEnergyImported)]);
    this.dailyownuseddata.push([value.createdAt, Number(value.dailyEnergySelfCovered)]);
    this.dailyconsumeddata.push([value.createdAt, Number(value.dailyEnergyConsumed)]);
    this.dailyproduceddata.push([value.createdAt, Number(value.dailyEnergyYield)]);
  }

  private compareArrays(a: Number[], b: Number[]): number {
    if (a[0] < b[0]) { return -1; }
    if (a[0] > b[0]) { return 1; }
    return 0;
  }

  private compareNumbers(a: Number, b: Number): number {
    if (a < b) { return -1; }
    if (a > b) { return 1; }
    return 0;
  }

  private sortData() {
    console.log("sorting data");
    this.solarpowerdata.sort((a, b) => this.compareArrays(a, b));
    this.gridpowerdata.sort((a, b) => this.compareArrays(a, b));
    this.usedpowerdata.sort((a, b) => this.compareArrays(a, b));
    this.dailyexporteddata.sort((a, b) => this.compareArrays(a, b));
    this.dailyimporteddata.sort((a, b) => this.compareArrays(a, b));
    this.dailyownuseddata.sort((a, b) => this.compareArrays(a, b));
    this.dailyconsumeddata.sort((a, b) => this.compareArrays(a, b));
    this.dailyproduceddata.sort((a, b) => this.compareArrays(a, b));
  }
}
