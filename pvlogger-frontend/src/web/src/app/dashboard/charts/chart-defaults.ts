import * as Highcharts from 'highcharts';
import { DateUtil } from '../../service/date.util';

export function createTimeXAxis(date: number): Highcharts.XAxisOptions {
  return {
    title: { text: 'Date' },
    type: 'datetime',
    showFirstLabel: true,
    showLastLabel: true,
    // Highcharts 13 formats the midnight tick of an hourly axis from `boundary`;
    // without it that tick reads "12:00 AM" instead of the date.
    dateTimeLabelFormats: { day: { main: '%e %b %y', boundary: '%e %b %y' } },
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
