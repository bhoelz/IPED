import {
  ChangeDetectionStrategy, Component, computed, HostListener, inject,
  Input, OnChanges, signal,
} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {IslandBase} from '../shared/island-base';
import {islandErrorEvent, viewerReadyEvent, ViewerType} from '../shared/events';
import {HexViewerComponent} from '../hex-viewer/hex-viewer.component';

/**
 * Viewer-host island (`<iped-viewer>`).
 *
 * <p>Routes to the appropriate sub-renderer based on MIME type:
 * <ul>
 *   <li>{@code text/*}, {@code application/json}, {@code application/xml} → text/HTML renderer</li>
 *   <li>{@code image/*} → native {@code <img>}</li>
 *   <li>{@code application/pdf} → {@code <embed>} PDF renderer</li>
 *   <li>anything else → hex renderer (delegates to {@link HexViewerComponent})</li>
 * </ul>
 *
 * <p>Boundary contract:
 * <ul>
 *   <li>Inputs: {@code item-id} ("{sourceId}:{docId}"), {@code media-type}, {@code api-base},
 *       {@code highlight} (space-separated search terms for text mode)</li>
 *   <li>Events out: {@code viewer-ready}, {@code island-error}</li>
 *   <li>Commands in: {@code previous-page}, {@code next-page} (forwarded to hex sub-renderer)</li>
 * </ul>
 */
@Component({
  selector: 'iped-viewer-impl',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [HexViewerComponent],
  templateUrl: './viewer.component.html',
  styleUrl: './viewer.component.scss',
})
export class ViewerComponent extends IslandBase implements OnChanges {
  private readonly http = inject(HttpClient);

  @Input('item-id')   itemId    = '';
  @Input('media-type') mediaType = '';
  @Input('highlight') highlight  = '';
  @Input('api-base') override apiBase = '/api';

  protected readonly loading     = signal(false);
  protected readonly error       = signal<string | null>(null);
  protected readonly textContent = signal<string>('');
  protected readonly htmlContent = signal<string>('');

  protected readonly viewerType = computed<ViewerType>(() => resolveViewerType(this.mediaType));

  /** URL used by image/pdf renderers — direct to ContentV2, no Range needed. */
  protected readonly contentUrl = computed(() => {
    if (!this.itemId) return '';
    const [src, ...rest] = this.itemId.split(':');
    const docId = rest.join(':');
    return `${this.apiBase.replace(/\/$/, '')}/v2/sources/${src}/items/${docId}/content`;
  });

  /** URL used by text renderer, with highlight param appended when present. */
  protected readonly textUrl = computed(() => {
    if (!this.itemId) return '';
    const [src, ...rest] = this.itemId.split(':');
    const docId = rest.join(':');
    let url = `${this.apiBase.replace(/\/$/, '')}/v2/sources/${src}/items/${docId}/text`;
    if (this.highlight) url += `?highlight=${encodeURIComponent(this.highlight)}`;
    return url;
  });

  @HostListener('previous-page') onPrev(): void {
    // forwarded to hex-viewer via host event re-dispatch
    this.hostEl.querySelector('iped-hex-viewer-impl')
      ?.dispatchEvent(new CustomEvent('previous-page'));
  }

  @HostListener('next-page') onNext(): void {
    this.hostEl.querySelector('iped-hex-viewer-impl')
      ?.dispatchEvent(new CustomEvent('next-page'));
  }

  ngOnChanges(): void {
    this.error.set(null);
    this.textContent.set('');
    this.htmlContent.set('');

    const type = this.viewerType();
    if (!this.itemId) return;

    if (type === 'text') {
      this.loadText(false);
    } else if (type === 'html') {
      this.loadText(true);
    } else if (type === 'image' || type === 'pdf' || type === 'hex') {
      // content URL is a computed signal; just dispatch ready
      this.dispatchReady(type);
    } else {
      // unsupported — nothing to load
    }
  }

  private loadText(wantHtml: boolean): void {
    this.loading.set(true);
    const accept = wantHtml ? 'text/html' : 'text/plain';
    this.http.get(this.textUrl(), {responseType: 'text', headers: {Accept: accept}}).subscribe({
      next: (body) => {
        this.loading.set(false);
        if (wantHtml) {
          this.htmlContent.set(body);
        } else {
          this.textContent.set(body);
        }
        this.dispatchReady(wantHtml ? 'html' : 'text');
      },
      error: (e) => {
        this.loading.set(false);
        const msg = e?.error?.message ?? e?.message ?? 'Failed to load text';
        this.error.set(msg);
        this.dispatch(islandErrorEvent({island: 'viewer', message: msg, cause: e}));
      },
    });
  }

  private dispatchReady(type: ViewerType): void {
    this.dispatch(viewerReadyEvent({itemId: this.itemId, viewerType: type, mediaType: this.mediaType}));
  }

  protected get hexItemId(): string { return this.itemId; }
}

function resolveViewerType(mediaType: string): ViewerType {
  const m = (mediaType ?? '').toLowerCase();
  if (!m)                        return 'hex';
  if (m.startsWith('image/'))    return 'image';
  if (m === 'application/pdf')   return 'pdf';
  if (m === 'text/html')         return 'html';
  if (m.startsWith('text/'))     return 'text';
  if (m === 'application/json')  return 'text';
  if (m === 'application/xml')   return 'text';
  // Binary or unknown → hex
  return 'hex';
}
