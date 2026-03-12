import { Component, OnInit, OnDestroy } from '@angular/core';
import * as Highcharts from 'highcharts';
import { Subscription } from 'rxjs';
import { AppBus } from '../../../service/app-bus.service';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';
import { createTimeXAxis } from '../chart-defaults';
import { HighchartsChartComponent } from 'highcharts-angular';


@Component({
    selector: 'app-kpichart',
    templateUrl: './kpi-chart.component.html',
    styleUrls: ['./kpi-chart.component.sass'],
    imports: [HighchartsChartComponent]
})
export class KpiChartComponent implements OnInit, OnDestroy {
  title = 'PV Logger';
  chartOptions: Highcharts.Options = {};
  private kpiSubscription: Subscription | undefined;

  constructor(private kpiDataSource: KpiDataSourceService, private appBus: AppBus) {}

  ngOnInit(): void {
    this.kpiSubscription = this.appBus.kpiValues.subscribe((value) => {
      this.getChart(value.createdAt);
    });
  }

  ngOnDestroy(): void {
    this.kpiSubscription?.unsubscribe();
  }

// https://www.highcharts.com/demo/line-time-series
  getChart(date: number) {
    this.chartOptions = {
      series: [{
        type: 'line',
        name: 'Inverter Power',
        data: this.kpiDataSource.getSolarPowerData(),
        color: '#7bb5ec',
      }, {
        type: 'line',
        name: 'Consumed Power',
        data: this.kpiDataSource.getUsedPowerData(),
        color: '#f7a35c',
      }, {
        type: 'area',
        name: 'Grid Power',
        data: this.kpiDataSource.getGridPowerData(),
        zones: [{
          value: 0,
          color: {
            linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
            stops: [
              [0, new Highcharts.Color('#00f942').setOpacity(0).get('rgba') as string],
              [1, new Highcharts.Color('#00f942').get('rgba') as string],
            ]
          }
        }, {
          color: {
            linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
            stops: [
              [0, new Highcharts.Color('#ff2600').get('rgba') as string],
              [1, new Highcharts.Color('#ff2600').setOpacity(0).get('rgba') as string]
            ]
          }
        }]
      },],
      chart: {
        type: 'line',
        zooming: { type: 'x' },
      },
      title: {text: undefined},
      tooltip: {
        split: true,
        pointFormat: '<span style="color:{series.color}">{series.name}</span>: {point.y:,.2f}kW<br/>'
      },
      xAxis: createTimeXAxis(date),
      yAxis: {
        title: {text: undefined},
        labels: {format: '{value} kW'}
      },
    };
  }
}
