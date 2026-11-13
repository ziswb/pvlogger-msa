import { Component, OnInit, OnDestroy, ChangeDetectionStrategy } from '@angular/core';
import * as Highcharts from 'highcharts';
import { Subscription } from 'rxjs';
import { AppBus } from '../../../service/app-bus.service';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';
import { createTimeXAxis, createKwhTooltip } from '../chart-defaults';
import { HighchartsChartComponent } from 'highcharts-angular';

/**
 * The day's cumulative energy KPIs so far, mirroring the Grafana panel
 * "Today's KPIs so far": consumed, produced, imported, exported (drawn below
 * zero) and self-consumed, unstacked, each a lightly filled line.
 */
@Component({
    selector: 'app-daily-kpi-chart',
    templateUrl: './daily-kpi-chart.component.html',
    styleUrls: ['./daily-kpi-chart.component.sass'],
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [HighchartsChartComponent]
})
export class DailyKpiChartComponent implements OnInit, OnDestroy {
  chartOptions: Highcharts.Options = {};
  private kpiSubscription: Subscription | undefined;

  constructor(private kpiDataSource: KpiDataSourceService, private appBus: AppBus) {}

  ngOnInit() {
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

  getChart(date: number) {
    this.chartOptions = {
      series: [{
        type: 'area',
        name: 'Consumed',
        data: this.kpiDataSource.getDailyConsumedData(),
        color: '#f7a35c',
      }, {
        type: 'area',
        name: 'Produced',
        data: this.kpiDataSource.getDailyProducedData(),
        color: '#7bb5ec',
      }, {
        type: 'area',
        name: 'Imported',
        data: this.kpiDataSource.getDailyImportedData(),
        color: '#ff2600',
      }, {
        type: 'area',
        name: 'Exported',
        data: this.kpiDataSource.getDailyExportedData(),
        color: '#00f942',
      }, {
        type: 'area',
        name: 'Self-Consumed',
        data: this.kpiDataSource.getDailyOwnusedData(),
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
          // Grafana's line style: no point markers unless hovered.
          marker: { enabled: false },
          // Exported is negative, so fill towards zero, not the axis bottom.
          threshold: 0,
        },
      },
      title: {text: undefined},
      tooltip: createKwhTooltip(),
      xAxis: createTimeXAxis(date),
      yAxis: {
        title: {text: undefined},
        labels: {format: '{value} kWh'},
        plotLines: [{ value: 0, width: 1, color: '#ccd6eb', zIndex: 3 }],
      },
    };
  }
}
