import * as Highcharts from 'highcharts';
import { DateUtil } from '../../service/date.util';

Highcharts.setOptions({
  time: {
    getTimezoneOffset: (timestamp: number) => new Date(timestamp).getTimezoneOffset(),
  },
});

export function createTimeXAxis(date: number): Highcharts.XAxisOptions {
  return {
    title: { text: 'Date' },
    type: 'datetime',
    showFirstLabel: true,
    showLastLabel: true,
    dateTimeLabelFormats: { day: '%e %b %y' },
    min: new DateUtil(date).minDate(),
    max: new DateUtil(date).maxDate(),
  };
}

export function createKwhTooltip(): Highcharts.TooltipOptions {
  return {
    split: true,
    pointFormat: '<span style="color:{series.color}">{series.name}</span>: {point.y:,.1f}kWh<br/>',
  };
}
