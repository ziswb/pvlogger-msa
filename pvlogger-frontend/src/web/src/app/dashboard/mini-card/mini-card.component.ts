import { Component, Input, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import { Subscription } from 'rxjs';
import { AppBus } from '../../service/app-bus.service';
import { KpiValues } from '../../model/kpi-values.model';
import { DecimalPipe, DatePipe } from '@angular/common';
import { MatCard, MatCardHeader, MatCardTitle, MatCardContent, MatCardFooter, MatCardSubtitle } from '@angular/material/card';

@Component({
    selector: 'app-mini-card',
    templateUrl: './mini-card.component.html',
    styleUrls: ['./mini-card.component.scss'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [MatCard, MatCardHeader, MatCardTitle, MatCardContent, MatCardFooter, MatCardSubtitle, DecimalPipe, DatePipe]
})
export class MiniCardComponent implements OnInit, OnDestroy {
  @Input() title = 'PV Produktion';
  @Input() colorClass = 'pvprod';
  @Input() kwFn: (value: KpiValues) => number = () => 0.0;
  @Input() kWhFn: (value: KpiValues) => number = () => 0.0;
  kwValue = 0;
  kWhValue = 0;
  lastUpdated: number = Date.now();
  private kpiSubscription: Subscription | undefined;

  constructor(private appBus: AppBus) {
  }

  ngOnInit(): void {
    this.kpiSubscription = this.appBus.kpiValues.subscribe((value) => {
      this.kwValue = this.kwFn(value);
      this.kWhValue = this.kWhFn(value);
      this.lastUpdated = value.createdAt;
    });
  }

  ngOnDestroy(): void {
    this.kpiSubscription?.unsubscribe();
  }
}
