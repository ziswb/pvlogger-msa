import { HttpClientModule } from '@angular/common/http';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MAT_DATE_LOCALE, MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { BrowserModule } from '@angular/platform-browser';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { HighchartsChartModule } from 'highcharts-angular';
import { AgGridModule } from 'ag-grid-angular';
import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { CardComponent } from './dashboard/card/card.component';
import { ImportExportChartComponent } from './dashboard/charts/import-export-chart/import-export-chart.component';
import { ImportOwnuseChartComponent } from './dashboard/charts/import-ownuse-chart/import-ownuse-chart.component';
import { KpiChartComponent } from './dashboard/charts/kpi-chart/kpi-chart.component';
import { ProducedConsumedChartComponent } from './dashboard/charts/produced-consumed-chart/produced-consumed-chart.component';
import { ConsumedImportedChart } from './dashboard/charts/consumed-imported-chart/consumed-imported-chart';
import { DashboardComponent } from './dashboard/dashboard.component';
import { MiniCardComponent } from './dashboard/mini-card/mini-card.component';
import { DatepickerComponent } from './datepicker/datepicker.component';

@NgModule({
  declarations: [
    AppComponent,
    KpiChartComponent,
    DashboardComponent,
    MiniCardComponent,
    CardComponent,
    ImportExportChartComponent,
    ProducedConsumedChartComponent,
    ImportOwnuseChartComponent,
    ImportOwnuseChartComponent,
    ConsumedImportedChart,
    DatepickerComponent,
  ],
  imports: [
    BrowserModule,
    AppRoutingModule,
    HttpClientModule,
    HighchartsChartModule,
    BrowserAnimationsModule,
    MatGridListModule,
    MatCardModule,
    MatListModule,
    MatNativeDateModule,
    MatInputModule,
    MatDatepickerModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    ReactiveFormsModule,
    AgGridModule,
  ],
  providers: [
    { provide: MAT_DATE_LOCALE, useValue: 'de-CH' }
  ],
  bootstrap: [AppComponent]
})
export class AppModule {
}
