import { Component, Input, OnInit } from '@angular/core';
import { AppBus } from '../../service/app-bus.service';
import { DecimalPipe, DatePipe } from '@angular/common';
import { MatList, MatListItem } from '@angular/material/list';
import { MatCard, MatCardHeader, MatCardTitle, MatCardContent, MatCardFooter, MatCardSubtitle } from '@angular/material/card';

@Component({
    selector: 'app-mini-card',
    templateUrl: './mini-card.component.html',
    styleUrls: ['./mini-card.component.scss'],
    imports: [MatCard, MatCardHeader, MatCardTitle, MatCardContent, MatList, MatListItem, MatCardFooter, MatCardSubtitle, DecimalPipe, DatePipe]
})
export class MiniCardComponent implements OnInit {
  @Input() title: String = 'PV Produktion';
  @Input() colorClass: String = 'pvprod';
  @Input() kwFn: (value: any) => Number = (value) => 0.0;
  @Input() kWhFn: (value: any) => Number = (value) => 0.0;
  kwValue: Number = 0;
  kWhValue: Number = 0;
  lastUpdated: number = Date.now();

  constructor(private appBus: AppBus) {
  }

  ngOnInit(): void {
    this.appBus.kpiValues.subscribe((value) => {
      this.kwValue = this.kwFn(value);
      this.kWhValue = this.kWhFn(value);
      this.lastUpdated = value.createdAt;
    });
  }

}
