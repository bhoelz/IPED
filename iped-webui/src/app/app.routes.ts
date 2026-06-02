import {Routes} from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'workspace'
  },
  {
    path: 'workspace',
    loadComponent: () =>
      import('./domains/workspace/pages/workspace-page').then(
        (module) => module.WorkspacePage
      )
  },
  {
    path: '**',
    redirectTo: 'workspace'
  }
];
