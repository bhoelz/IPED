import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { Configuration } from './core/api/generated/configuration';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    {
      provide: Configuration,
      useValue: new Configuration({
        basePath: 'http://localhost:8080'
      })
    },
    provideRouter(routes)
  ]
};
