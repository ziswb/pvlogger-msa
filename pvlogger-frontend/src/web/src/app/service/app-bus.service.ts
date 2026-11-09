import { Injectable } from '@angular/core';
import { animationFrameScheduler, auditTime, ReplaySubject } from "rxjs";
import { KpiValues } from '../model/kpi-values.model';

@Injectable({
  providedIn: 'root'
})
export class AppBus {
  private kpiValuesSubject = new ReplaySubject<KpiValues>(1);
  private dateSubject = new ReplaySubject<Date>(1);
  // At most one update per rendered frame: a hidden tab renders no frames, so it
  // stops redrawing the charts, and values queued while the browser paused it
  // are drawn once, not one full redraw each (which froze the page).
  public readonly kpiValues = this.kpiValuesSubject.pipe(auditTime(0, animationFrameScheduler));
  public readonly dateChanged = this.dateSubject.asObservable();

  publishDateChanged(date: Date) {
    this.dateSubject.next(date);
  }

  publishKpiValues(values: KpiValues) {
    this.kpiValuesSubject.next(values);
  }
}
