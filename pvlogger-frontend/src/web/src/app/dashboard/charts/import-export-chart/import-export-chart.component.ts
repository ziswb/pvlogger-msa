import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import * as Highcharts from 'highcharts';
import { Subscription } from 'rxjs';
import { AppBus } from '../../../service/app-bus.service';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';
import { createTimeXAxis, createKwhTooltip } from '../chart-defaults';
import { HighchartsChartComponent } from 'highcharts-angular';


@Component({
    selector: 'app-import-export-chart',
    templateUrl: './import-export-chart.component.html',
    styleUrls: ['./import-export-chart.component.sass'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [HighchartsChartComponent]
})
export class ImportExportChartComponent implements OnInit, OnDestroy {
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
        name: 'Exported Power',
        data: this.kpiDataSource.getDailyExportedData(),
        color: '#00f942',
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
