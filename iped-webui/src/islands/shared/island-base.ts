import {Directive, ElementRef, inject, OnDestroy} from '@angular/core';
import {API_BASE_URL} from './api';

/**
 * Abstract base for all IPED island components.
 *
 * Provides:
 * - `apiBase` resolved from the {@link API_BASE_URL} token (overridable per-island
 *   via the `api-base` attribute, which Angular Elements maps to `apiBase` input).
 * - `dispatch()` — typed helper for firing CustomEvents on the host element.
 * - `hostEl` — the raw host element, available for direct DOM access when needed.
 *
 * Usage:
 * ```ts
 * @Component({...})
 * export class MyIsland extends IslandBase {
 *   protected doSomething(): void {
 *     this.dispatch(itemSelectedEvent({sourceId, itemId}));
 *   }
 * }
 * ```
 */
@Directive()
export abstract class IslandBase implements OnDestroy {
  protected readonly hostEl: HTMLElement = inject(ElementRef<HTMLElement>).nativeElement;

  /** Resolved API base URL; islands may override via an `api-base` input attribute. */
  protected apiBase: string = inject(API_BASE_URL);

  /** Dispatches a CustomEvent on the host element. */
  protected dispatch<T>(event: CustomEvent<T>): void {
    this.hostEl.dispatchEvent(event);
  }

  ngOnDestroy(): void {
    // Subclasses override when cleanup is needed; base is intentionally empty.
  }
}
