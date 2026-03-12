import { Component, OnInit, OnDestroy } from '@angular/core';
import * as Highcharts from 'highcharts';
import { Subscription } from 'rxjs';
import { AppBus } from '../../../service/app-bus.service';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';
import { DateUtil } from '../../../service/date.util';
import { createTimeXAxis } from '../chart-defaults';
import { HighchartsChartComponent } from 'highcharts-angular';


@Component({
    selector: 'app-import-ownuse-chart',
    templateUrl: './import-ownuse-chart.component.html',
    styleUrls: ['./import-ownuse-chart.component.sass'],
    imports: [HighchartsChartComponent]
})
export class ImportOwnuseChartComponent implements OnInit, OnDestroy {
  chartOptions: Highcharts.Options = {};
  private kpiSubscription: Subscription | undefined;

  constructor(private kpiDataSource: KpiDataSourceService, private appBus: AppBus) {}

  ngOnInit() {
    this.kpiSubscription = this.appBus.kpiValues.subscribe((value) => {
      this.getChart(value.createdAt);
    });
  }

  ngOnDestroy(): void {
    this.kpiSubscription?.unsubscribe();
  }

  // https://www.highcharts.com/demo/area-stacked
  getChart(date: number) {
    this.chartOptions = {
      series: [{
        type: 'area',
        name: 'Imported Power',
        data: this.kpiDataSource.getDailyImportedData(),
        color: '#ff2600',
      }, {
        type: 'area',
        name: 'Self Consumed Power',
        data: this.kpiDataSource.getDailyOwnusedData(),
        color: '#f7a35c',
      }],
      chart: {
        type: 'area',
        zooming: { type: 'x' },
      },
      title: {text: undefined},
      tooltip: {
        pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.percentage:.1f}%</b> ({point.y:,.1f}kWh)<br/>',
        split: true
      },
      xAxis: createTimeXAxis(date),
      plotOptions: {
        area: {stacking: 'percent'},
        series: {
          marker: {enabled: false},
          pointStart: new DateUtil(date).minDate()
        }
      },
      yAxis: {
        title: {text: undefined},
        labels: {format: '{value}%'}
      },
    };
  }
}
