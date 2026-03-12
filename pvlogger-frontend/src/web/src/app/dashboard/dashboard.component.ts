import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { Component } from '@angular/core';
import { map } from 'rxjs/operators';
import { KpiValues } from '../model/kpi-values.model';
import { ConsumedImportedChart } from './charts/consumed-imported-chart/consumed-imported-chart';
import { ImportExportChartComponent } from './charts/import-export-chart/import-export-chart.component';
import { ProducedConsumedChartComponent } from './charts/produced-consumed-chart/produced-consumed-chart.component';
import { ImportOwnuseChartComponent } from './charts/import-ownuse-chart/import-ownuse-chart.component';
import { KpiChartComponent } from './charts/kpi-chart/kpi-chart.component';
import { CardComponent } from './card/card.component';
import { MiniCardComponent } from './mini-card/mini-card.component';
import { AsyncPipe } from '@angular/common';
import { MatGridList, MatGridTile } from '@angular/material/grid-list';
import { DatepickerComponent } from '../datepicker/datepicker.component';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.sass'],
    imports: [DatepickerComponent, MatGridList, MatGridTile, MiniCardComponent, CardComponent, KpiChartComponent, ImportOwnuseChartComponent, ProducedConsumedChartComponent, ImportExportChartComponent, ConsumedImportedChart, AsyncPipe]
})
export class DashboardComponent {
  miniCardData: MiniCardModel[] = [
    new MiniCardModel('Energieverbrauch', 'pv-usage', (value) => value.ownConsumption, (value) => value.dailyEnergyConsumed),
    new MiniCardModel('Energieimport', 'pv-import', (value) => value.gridToHousePower, (value) => value.dailyEnergyImported),
    new MiniCardModel('PV Produktion', 'pv-prod', (value) => value.solarPower, (value) => value.dailyEnergyYield),
    new MiniCardModel('Energieexport', 'pv-export', (value) => value.solarToGridPower, (value) => value.dailyEnergyExported),
  ];
  /** Based on the screen size, switch from standard to one column per row */
  cardLayout = this.breakpointObserver.observe([Breakpoints.Handset, Breakpoints.Tablet]).pipe(
    map(({matches}) => {
      if (matches) {
        if (this.breakpointObserver.isMatched(Breakpoints.Handset)) {
          return this.handsetLayout();
        }
        if (this.breakpointObserver.isMatched(Breakpoints.Tablet)) {
          return this.tabletLayout();
        }
      }
      return this.defaultLayout();
    })
  );

  private handsetLayout() {
    return {
      columns: 1,
      miniCard: {cols: 1, rows: 1},
      chart: {cols: 1, rows: 3},
      table: {cols: 1, rows: 4},
    };
  }

  private tabletLayout() {
    return {
      columns: 2,
      miniCard: {cols: 1, rows: 1},
      chart: {cols: 2, rows: 3},
      table: {cols: 2, rows: 3},
    };
  }

  private defaultLayout() {
    return {
      columns: 4,
      miniCard: {cols: 1, rows: 1},
      chart: {cols: 2, rows: 3},
      table: {cols: 4, rows: 3},
    };
  }

  constructor(private breakpointObserver: BreakpointObserver) {
  }
}

class MiniCardModel {
  constructor(
    public title: string,
    public colorClass: string,
    public kwFn: (value: KpiValues) => number,
    public kWhFn: (value: KpiValues) => number,
  ) {}
}
