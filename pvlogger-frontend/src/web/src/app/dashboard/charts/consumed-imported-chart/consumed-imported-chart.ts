import { Component, OnInit } from '@angular/core';
import { AppBus } from '../../../service/app-bus.service';
import { DateUtil } from '../../../service/date.util';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';
import { HighchartsChartComponent } from 'highcharts-angular';


@Component({
    selector: 'app-consumed-imported-chart',
    templateUrl: './consumed-imported-chart.html',
    styleUrls: ['./consumed-imported-chart.sass'],
    imports: [HighchartsChartComponent]
})
export class ConsumedImportedChart implements OnInit {
  chartOptions: any;

  constructor(private kpiDataSource: KpiDataSourceService, private appBus: AppBus) {}

  ngOnInit() {
    this.appBus.kpiValues.subscribe((value) => {
      this.getChart(value.createdAt);
    });
  }

  // https://www.highcharts.com/demo/area-stacked
  getChart(date: number) {
    this.chartOptions = {
      series: [{
        name: 'Consumed Power',
        data: this.kpiDataSource.getDailyConsumedData(),
        color: '#a9a742',
      }, {
        name: 'Imported Power',
        data: this.kpiDataSource.getDailyImportedData(),
        color: '#ff2600',
      }],
      chart: {
        type: 'area',
        zoomType: 'x',
      },
      title: {text: undefined},
      tooltip: {
        split: true,
        pointFormat: '<span style="color:{series.color}">{series.name}</span>: {point.y:,.1f}kWh<br/>'
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
        labels: {format: '{value} kWh'}
      },
      accessibility: {enabled: false}
    };
  }
}
