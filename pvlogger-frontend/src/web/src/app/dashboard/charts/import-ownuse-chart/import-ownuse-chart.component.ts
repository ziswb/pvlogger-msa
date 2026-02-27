import { Component, OnInit } from '@angular/core';
import { AppBus } from '../../../service/app-bus.service';
import { DateUtil } from '../../../service/date.util';
import { KpiDataSourceService } from '../../../service/kpi-data-source.service';
import { HighchartsChartComponent } from 'highcharts-angular';


@Component({
    selector: 'app-import-ownuse-chart',
    templateUrl: './import-ownuse-chart.component.html',
    styleUrls: ['./import-ownuse-chart.component.sass'],
    imports: [HighchartsChartComponent]
})
export class ImportOwnuseChartComponent implements OnInit {
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
        name: 'Imported Power',
        data: this.kpiDataSource.getDailyImportedData(),
        color: '#ff2600',
      }, {
        name: 'Self Consumed Power',
        data: this.kpiDataSource.getDailyOwnusedData(),
        color: '#f7a35c',
      }],
      chart: {
        type: 'area',
        zoomType: 'x',
      },
      title: {text: undefined},
      tooltip: {
        pointFormat: '<span style="color:{series.color}">{series.name}</span>: <b>{point.percentage:.1f}%</b> ({point.y:,.1f}kWh)<br/>',
        split: true
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
      plotOptions: {
        area: {stacking: 'percent'},
        marker: {enabled: false},
        series: {pointStart: new DateUtil(date).minDate()}
      },
      yAxis: {
        title: {text: undefined},
        labels: {format: '{value}%'}
      },
      accessibility: {enabled: false}
    };
  }
}
