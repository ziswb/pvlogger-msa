import * as Highcharts from 'highcharts';
import { createTimeXAxis } from './chart-defaults';

describe('createTimeXAxis', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    container = document.createElement('div');
    container.style.width = '1200px';
    document.body.appendChild(container);
  });

  afterEach(() => container.remove());

  // Highcharts 13 formats the midnight tick of an hourly axis from
  // dateTimeLabelFormats.day.boundary; without it the label read "12:00 AM".
  it('labels the midnight tick with the date', () => {
    const day = new Date(2026, 10, 9).getTime();
    const chart = Highcharts.chart(container, {
      // Same time zone handling as main.ts.
      time: { timezone: Intl.DateTimeFormat().resolvedOptions().timeZone },
      xAxis: createTimeXAxis(day),
      series: [{ type: 'line', data: [[day + 6 * 3600_000, 1], [day + 20 * 3600_000, 2]] }],
    });

    // Read the tick labels from the axis, not the DOM: jsdom has no text
    // measurement, so Highcharts leaves a probe <text> among the labels there.
    const axis = chart.xAxis[0] as Highcharts.Axis & { ticks: Record<number, { label?: { textStr?: string } }> };
    const first = axis.tickPositions![0];
    expect(first).toBe(day);
    expect(axis.ticks[first].label?.textStr?.trim()).toBe('9 Nov 26');
    chart.destroy();
  });
});
