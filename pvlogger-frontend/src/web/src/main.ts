import { enableProdMode, importProvidersFrom, provideZoneChangeDetection } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { MAT_DATE_LOCALE, MatNativeDateModule } from '@angular/material/core';
import { provideHighcharts } from 'highcharts-angular';

import { environment } from './environments/environment';
import { AppComponent } from './app/app.component';
import { DashboardComponent } from './app/dashboard/dashboard.component';

if (environment.production) {
  enableProdMode();
}

bootstrapApplication(AppComponent, {
    providers: [
        // Angular 21 defaults to zoneless; the app still relies on zone.js change detection.
        provideZoneChangeDetection(),
        provideRouter([
            { path: '', component: DashboardComponent },
        ]),
        importProvidersFrom(MatNativeDateModule),
        { provide: MAT_DATE_LOCALE, useValue: 'de-CH' },
        provideHttpClient(withInterceptorsFromDi()),
        provideAnimations(),
        ...provideHighcharts({
            options: {
                time: {
                    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
                },
            },
        }),
    ]
})
  .catch(err => console.error(err));
