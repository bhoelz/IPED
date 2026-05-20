import { Injectable, signal } from '@angular/core';

import { Configuration } from '../api/generated/configuration';

const STORAGE_KEY = 'iped.webui.api.basePath';
const DEFAULT_BASE_PATH = 'http://localhost:8080';

@Injectable({
  providedIn: 'root'
})
export class WebApiRuntimeConfigService {
  readonly basePath = signal(this.readInitialBasePath());

  constructor(private readonly configuration: Configuration) {
    this.configuration.basePath = this.basePath();
  }

  updateBasePath(nextBasePath: string) {
    const normalized = this.normalize(nextBasePath);

    this.basePath.set(normalized);
    this.configuration.basePath = normalized;

    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, normalized);
    }
  }

  private readInitialBasePath() {
    if (typeof localStorage === 'undefined') {
      return DEFAULT_BASE_PATH;
    }

    return this.normalize(localStorage.getItem(STORAGE_KEY) ?? DEFAULT_BASE_PATH);
  }

  private normalize(value: string) {
    const trimmed = value.trim();

    return trimmed.length > 0 ? trimmed.replace(/\/+$/, '') : DEFAULT_BASE_PATH;
  }
}
