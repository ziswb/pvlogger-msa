import { Component, OnInit, OnDestroy } from '@angular/core';
import * as Highcharts from 'highcharts';
import { Subscription } from 'rxjs';
import { AppBus } from '../../../service/app-bus.service';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';
import { createTimeXAxis, createKwhTooltip } from '../chart-defaults';
import { HighchartsChartComponent } from 'highcharts-angular';


@Component({
    selector: 'app-consumed-imported-chart',
    templateUrl: './consumed-imported-chart.html',
    styleUrls: ['./consumed-imported-chart.sass'],
    imports: [HighchartsChartComponent]
})
export class ConsumedImportedChart implements OnInit, OnDestroy {
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
        name: 'Consumed Power',
        data: this.kpiDataSource.getDailyConsumedData(),
        color: '#a9a742',
      }, {
        type: 'area',
        name: 'Imported Power',
        data: this.kpiDataSource.getDailyImportedData(),
        color: '#ff2600',
      }],
      chart: {
        type: 'area',
        zooming: { type: 'x' },
      },
      title: {text: undefined},
      tooltip: createKwhTooltip(),
      xAxis: createTimeXAxis(date),
      yAxis: {
        title: {text: undefined},
        labels: {format: '{value} kWh'}
      },
    };
  }
}
