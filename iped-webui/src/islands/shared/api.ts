import {InjectionToken} from '@angular/core';

/**
 * DI token for the base URL of the iped-webapi backend.
 * Each island reads this so the host page can override it via a data attribute
 * without needing per-island wiring.
 *
 * Default: '/api' (development proxy target).
 */
export const API_BASE_URL = new InjectionToken<string>('IPED_API_BASE_URL', {
  providedIn: 'root',
  factory: () => '/api',
});
