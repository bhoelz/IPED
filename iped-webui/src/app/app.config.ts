import {ApplicationConfig, provideBrowserGlobalErrorListeners} from '@angular/core';
import {provideHttpClient} from '@angular/common/http';

import {Configuration} from './core/api/generated/configuration';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideHttpClient(),
    {
      provide: Configuration,
      useValue: new Configuration({
        basePath: 'http://localhost:8080'
      })
    }
  ]
};
