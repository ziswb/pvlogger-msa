import { Component, Input, OnInit } from '@angular/core';
import { AppBus } from '../../service/app-bus.service';

@Component({
  selector: 'app-mini-card',
  templateUrl: './mini-card.component.html',
  styleUrls: ['./mini-card.component.scss']
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
