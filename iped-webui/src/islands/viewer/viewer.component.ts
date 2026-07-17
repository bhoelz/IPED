import {
  ChangeDetectionStrategy, Component, computed, HostListener, inject,
  Input, OnChanges, signal, CUSTOM_ELEMENTS_SCHEMA,
} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {forkJoin} from 'rxjs';
import {IslandBase} from '../shared/island-base';
import {islandErrorEvent, viewerReadyEvent, ViewerType} from '../shared/events';
import {HexViewerComponent} from '../hex-viewer/hex-viewer.component';

interface AudioMeta {
  transcription: string | null;
  confidencePct: string | null;
  durationStr: string | null;
}

interface EmailData {
  subject: string | null;
  from: string | null;
  to: string | null;
  cc: string | null;
  bcc: string | null;
  date: string | null;
  body: string;
}

/**
 * Viewer-host island (`<iped-viewer>`).
 *
 * <p>Routes to the appropriate sub-renderer based on MIME type:
 * <ul>
 *   <li>{@code text/*}, {@code application/json}, {@code application/xml} → text/HTML renderer</li>
 *   <li>{@code image/*} → native {@code <img>}</li>
 *   <li>{@code application/pdf} → {@code <embed>} PDF renderer</li>
 *   <li>{@code audio/*} → native {@code <audio>} with optional transcription panel</li>
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
  schemas: [CUSTOM_ELEMENTS_SCHEMA],
  templateUrl: './viewer.component.html',
  styleUrl: './viewer.component.scss',
})
export class ViewerComponent extends IslandBase implements OnChanges {
  private readonly http = inject(HttpClient);

  @Input('item-id')   itemId    = '';
  @Input('media-type') mediaType = '';
  @Input('highlight') highlight  = '';
  @Input('companion-fallback') companionFallback = 'true';
  @Input('api-base') override apiBase = '/api';

  protected readonly loading          = signal(false);
  protected readonly error            = signal<string | null>(null);
  protected readonly textContent      = signal<string>('');
  protected readonly htmlContent      = signal<string>('');
  protected readonly audioMeta        = signal<AudioMeta | null>(null);
  protected readonly audioMetaLoading = signal(false);
  protected readonly emailData        = signal<EmailData | null>(null);
  protected readonly emailLoading     = signal(false);
  protected readonly searchQuery      = signal('');
  protected readonly searchHits       = signal<number[]>([]);
  protected readonly activeHit        = signal(-1);

  protected readonly viewerType = computed<ViewerType>(() => resolveViewerType(this.mediaType));

  private itemUrlBase(): string {
    if (!this.itemId) return '';
    const [src, ...rest] = this.itemId.split(':');
    const docId = rest.join(':');
    return `${this.apiBase.replace(/\/$/, '')}/v2/sources/${src}/items/${docId}`;
  }

  /** Raw content URL — direct stream from ContentV2 endpoint. */
  protected readonly contentUrl = computed(() =>
    this.itemId ? `${this.itemUrlBase()}/content` : ''
  );

  protected readonly companionUrl = computed(() =>
    this.itemId ? `iped-companion://open?item=${encodeURIComponent(this.itemId)}` : ''
  );

  protected companionEnabled(): boolean {
    return this.companionFallback !== 'false';
  }

  /**
   * Effective image URL — appends `?format=png` for TIFF types so the browser
   * can render the server-converted PNG instead of the raw TIFF bytes.
   */
  protected readonly imageUrl = computed(() => {
    const base = this.contentUrl();
    if (!base) return '';
    const m = (this.mediaType ?? '').toLowerCase();
    return (m === 'image/tiff' || m === 'image/x-tiff') ? `${base}?format=png` : base;
  });

  /**
   * Effective audio/video URL — appends `?transcode=webm` for codecs the browser
   * cannot natively decode (AMR, WMA, 3GPP, SILK, etc.) so the server transcodes
   * via ffmpeg before streaming. Browser-native types are served directly.
   */
  protected readonly mediaUrl = computed(() => {
    const base = this.contentUrl();
    if (!base) return '';
    const m = (this.mediaType ?? '').toLowerCase();
    if (m.startsWith('audio/') && !BROWSER_AUDIO_TYPES.has(m)) return `${base}?transcode=webm`;
    if (m.startsWith('video/') && !BROWSER_VIDEO_TYPES.has(m)) return `${base}?transcode=webm`;
    return base;
  });

  /** URL used by text renderer, with highlight param appended when present. */
  protected readonly textUrl = computed(() => {
    if (!this.itemId) return '';
    let url = `${this.itemUrlBase()}/text`;
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
    this.audioMeta.set(null);
    this.audioMetaLoading.set(false);
    this.emailData.set(null);
    this.emailLoading.set(false);
    this.searchHits.set([]);
    this.activeHit.set(-1);

    const type = this.viewerType();
    if (!this.itemId) return;

    if (type === 'text') {
      this.loadText(false);
    } else if (type === 'html') {
      this.loadText(true);
    } else if (type === 'audio') {
      this.dispatchReady(type);
      this.loadAudioMetadata();
    } else if (type === 'video') {
      this.dispatchReady(type);
    } else if (type === 'email') {
      this.dispatchReady(type);
      this.loadEmailData();
    } else if (type === 'image' || type === 'pdf' || type === 'hex') {
      this.dispatchReady(type);
    } else {
      // unsupported — nothing to load
    }
  }

  private loadAudioMetadata(): void {
    this.audioMetaLoading.set(true);
    this.http.get<{metadata?: Record<string, string[]>}>(this.itemUrlBase()).subscribe({
      next: (item) => {
        const meta = item.metadata ?? {};
        const rawTranscription = meta['audio:transcription']?.[0] ?? null;
        const rawConf          = meta['audio:transcriptConfidence']?.[0] ?? null;
        const rawDuration      = meta['audio:xmpDM:duration']?.[0] ?? null;

        let confidencePct: string | null = null;
        if (rawConf != null) {
          const c = parseFloat(rawConf);
          if (!isNaN(c)) confidencePct = `${Math.round(c * 100)}%`;
        }

        let durationStr: string | null = null;
        if (rawDuration != null) {
          const d = parseFloat(rawDuration);
          if (!isNaN(d) && d > 0) {
            const secs = Math.floor(d);
            const h = Math.floor(secs / 3600);
            const m = Math.floor(secs % 3600 / 60);
            const s = secs % 60;
            durationStr = h > 0 ? `${h}h ${m}m ${s}s` : m > 0 ? `${m}m ${s}s` : `${s}s`;
          }
        }

        this.audioMeta.set({transcription: rawTranscription, confidencePct, durationStr});
        this.audioMetaLoading.set(false);
      },
      error: () => {
        this.audioMeta.set({transcription: null, confidencePct: null, durationStr: null});
        this.audioMetaLoading.set(false);
      },
    });
  }

  private loadEmailData(): void {
    this.emailLoading.set(true);
    const meta$ = this.http.get<{metadata?: Record<string, string[]>}>(this.itemUrlBase());
    const body$ = this.http.get(this.textUrl(), {responseType: 'text', headers: {Accept: 'text/html'}});
    forkJoin({meta: meta$, body: body$}).subscribe({
      next: ({meta, body}) => {
        const m = meta.metadata ?? {};
        const first = (key: string) => m[key]?.[0] ?? null;
        const join  = (key: string) => m[key]?.join(', ') ?? null;
        this.emailData.set({
          subject: first('Message-Subject'),
          from:    join('Message:Message-From'),
          to:      join('Message:Message-To'),
          cc:      join('Message:Message-Cc'),
          bcc:     join('Message:Message-Bcc'),
          date:    first('dcterms:created'),
          body,
        });
        this.emailLoading.set(false);
      },
      error: () => {
        this.emailData.set({subject: null, from: null, to: null, cc: null, bcc: null, date: null, body: ''});
        this.emailLoading.set(false);
      },
    });
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

  protected searchViewer(event: Event): void {
    event.preventDefault();
    const query = this.searchQuery().trim().toLocaleLowerCase();
    const content = (this.viewerType() === 'html' ? this.htmlContent() : this.textContent()).toLocaleLowerCase();
    if (!query || !content) {
      this.searchHits.set([]);
      this.activeHit.set(-1);
      return;
    }
    const hits: number[] = [];
    let offset = 0;
    while ((offset = content.indexOf(query, offset)) >= 0 && hits.length < 10_000) {
      hits.push(offset);
      offset += query.length;
    }
    this.searchHits.set(hits);
    this.activeHit.set(hits.length ? 0 : -1);
    this.scrollToActiveHit();
  }

  protected nextSearchHit(): void {
    if (!this.searchHits().length) return;
    this.activeHit.update(i => (i + 1) % this.searchHits().length);
    this.scrollToActiveHit();
  }

  protected previousSearchHit(): void {
    if (!this.searchHits().length) return;
    this.activeHit.update(i => (i - 1 + this.searchHits().length) % this.searchHits().length);
    this.scrollToActiveHit();
  }

  private scrollToActiveHit(): void {
    const hit = this.activeHit();
    if (hit < 0) return;
    const node = this.hostEl.querySelector('.viewer-text, .viewer-html');
    if (node) node.scrollTop = Math.max(0, hit * 0.45);
  }

  protected dispatchReady(type: ViewerType): void {
    this.dispatch(viewerReadyEvent({itemId: this.itemId, viewerType: type, mediaType: this.mediaType}));
  }

  protected get hexItemId(): string { return this.itemId; }
}

/** Audio MIME types that browsers can decode natively (no server transcode needed). */
const BROWSER_AUDIO_TYPES = new Set([
  'audio/mpeg', 'audio/mp4', 'audio/aac', 'audio/ogg',
  'audio/wav', 'audio/wave', 'audio/flac', 'audio/x-flac', 'audio/webm',
]);

/** Video MIME types that browsers can decode natively (no server transcode needed). */
const BROWSER_VIDEO_TYPES = new Set([
  'video/mp4', 'video/webm', 'video/ogg',
]);

function resolveViewerType(mediaType: string): ViewerType {
  const m = (mediaType ?? '').toLowerCase();
  if (!m)                              return 'hex';
  // image/* — includes image/tiff (server converts to PNG via ?format=png)
  if (m.startsWith('image/'))          return 'image';
  if (m === 'application/pdf')         return 'pdf';
  if (m.startsWith('audio/'))          return 'audio';
  if (m.startsWith('video/'))          return 'video';
  if (m.startsWith('message/'))        return 'email';
  if (m === 'text/html')               return 'html';
  if (m.startsWith('text/'))           return 'text';
  if (m === 'application/json')        return 'text';
  if (m === 'application/xml')         return 'text';
  // MSG files are treated as email by the viewer
  if (m === 'application/vnd.ms-outlook') return 'email';
  // Binary or unknown → hex
  return 'hex';
}
