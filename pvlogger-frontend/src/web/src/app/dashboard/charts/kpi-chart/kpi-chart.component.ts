import { Component, OnInit } from '@angular/core';
import * as Highcharts from 'highcharts';
import { AppBus } from '../../../service/app-bus.service';
import { DateUtil } from '../../../service/date.util';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';

@Component({
  selector: 'app-kpichart',
  templateUrl: './kpi-chart.component.html',
  styleUrls: ['./kpi-chart.component.sass']
})
export class KpiChartComponent implements OnInit {
  title = 'PV Logger';
  Highcharts: typeof Highcharts = Highcharts;
  chartOptions: any;

  constructor(private kpiDataSource: KpiDataSourceService, private appBus: AppBus) {}

  ngOnInit(): void {
    this.appBus.kpiValues.subscribe((value) => {
      this.getChart(value.createdAt);
    });
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
              [0, new Highcharts.Color('#00f942').setOpacity(0).get('rgba')],
              [1, new Highcharts.Color('#00f942').get('rgba')],
            ]
          }
        }, {
          color: {
            linearGradient: {x1: 0, y1: 0, x2: 0, y2: 1},
            stops: [
              [0, new Highcharts.Color('#ff2600').get('rgba')],
              [1, new Highcharts.Color('#ff2600').setOpacity(0).get('rgba')]
            ]
          }
        }]
      },],
      chart: {
        type: "line",
        zoomType: 'x',
      },
      title: {text: undefined},
      tooltip: {
        split: true,
        pointFormat: '<span style="color:{series.color}">{series.name}</span>: {point.y:,.2f}kW<br/>'
      },
      xAxis: {
        title: {text: 'Date'},
        type: 'datetime',
        showFirstLabel: true,
        showLastLabel: true,
        dateTimeLabelFormats: {day: '%e %b %y'},
        min: new DateUtil(date).minDate(),
        max: new DateUtil(date).maxDate()
      },
      yAxis: {
        title: {text: undefined},
        labels: {format: '{value} kW'}
      },
      accessibility: {enabled: false}
    };
  }
}
