import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
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
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [HighchartsChartComponent]
})
export class KpiChartComponent implements OnInit, OnDestroy {
  title = 'PV Logger';
  chartOptions: Highcharts.Options = {};
  private kpiSubscription: Subscription | undefined;

  constructor(private kpiDataSource: KpiDataSourceService, private appBus: AppBus) {}

  ngOnInit(): void {
    // Build the full options now, not only on the first KPI value: that comes on an
    // animation frame, which a hidden tab never gets, and Highcharts' update() adds
    // no series to a chart created without them (backlog item 50).
    this.getChart(Date.now());
    this.kpiSubscription = this.appBus.kpiValues.subscribe((value) => {
      this.getChart(value.createdAt);
    });
  }

  ngOnDestroy(): void {
    this.kpiSubscription?.unsubscribe();
  }

/**
   * The current power values over the day: the same five KPIs as the
   * "Daily KPIs so far" chart, but live kW instead of cumulative kWh.
   */
  getChart(date: number) {
    this.chartOptions = {
      series: [{
        type: 'area',
        name: 'Consuming',
        data: this.kpiDataSource.getUsedPowerData(),
        color: '#f7a35c',
      }, {
        type: 'area',
        name: 'Producing',
        data: this.kpiDataSource.getSolarPowerData(),
        color: '#7bb5ec',
      }, {
        type: 'area',
        name: 'Importing',
        data: this.kpiDataSource.getImportedPowerData(),
        color: '#ff2600',
      }, {
        type: 'area',
        name: 'Exporting',
        data: this.kpiDataSource.getExportedPowerData(),
        color: '#00f942',
      }, {
        type: 'area',
        name: 'Self-Consuming',
        data: this.kpiDataSource.getSelfConsumedPowerData(),
        color: '#b388eb',
      }],
      chart: {
        type: 'area',
        zooming: { type: 'x' },
      },
      plotOptions: {
        area: {
          fillOpacity: 0.35,
          lineWidth: 1,
          marker: { enabled: false },
          // Exported is negative, so fill towards zero, not the axis bottom.
          threshold: 0,
        },
      },
      title: {text: undefined},
      tooltip: {
        split: true,
        pointFormat: '<span style="color:{series.color}">{series.name}</span>: {point.y:,.2f}kW<br/>'
      },
      xAxis: createTimeXAxis(date),
      yAxis: {
        title: {text: undefined},
        labels: {format: '{value} kW'},
        plotLines: [{ value: 0, width: 1, color: '#ccd6eb', zIndex: 3 }],
      },
    };
  }
}
