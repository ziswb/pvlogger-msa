import { Component, OnInit } from '@angular/core';
import { AppBus } from '../../../service/app-bus.service';
import { DateUtil } from '../../../service/date.util';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';
import { HighchartsChartComponent } from 'highcharts-angular';


@Component({
    selector: 'app-produced-consumed-chart',
    templateUrl: './produced-consumed-chart.component.html',
    styleUrls: ['./produced-consumed-chart.component.sass'],
    imports: [HighchartsChartComponent]
})
export class ProducedConsumedChartComponent implements OnInit {
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
        name: 'Produced Power',
        data: this.kpiDataSource.getDailyProducedData(),
        color: '#7bb5ec',
      }, {
        name: 'Self Consumed Power',
        data: this.kpiDataSource.getDailyOwnusedData(),
        color: '#f7a35c',
      }, {
        name: 'Exported Power',
        data: this.kpiDataSource.getDailyExportedData(),
        color: '#00f942',
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
