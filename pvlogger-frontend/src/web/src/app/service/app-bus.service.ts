import { Injectable } from '@angular/core';
import { ReplaySubject } from "rxjs";
import { KpiValues } from '../model/kpi-values.model';

@Injectable({
  providedIn: 'root'
})
export class AppBus {
  private kpiValuesSubject = new ReplaySubject<KpiValues>(1);
  private dateSubject = new ReplaySubject<Date>(1);
  public readonly kpiValues = this.kpiValuesSubject.asObservable();
  public readonly dateChanged = this.dateSubject.asObservable();

  publishDateChanged(date: Date) {
    this.dateSubject.next(date);
  }

  publishKpiValues(values: KpiValues) {
    this.kpiValuesSubject.next(values);
  }
}
