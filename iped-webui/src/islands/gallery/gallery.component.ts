import {HttpClient} from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  inject,
  Input,
  OnChanges,
  signal,
} from '@angular/core';
import {IslandBase} from '../shared/island-base';
import {
  itemSelectedEvent,
  resultsLoadedEvent,
  selectionChangedEvent,
  similarImageSearchEvent,
} from '../shared/events';

/**
 * Gallery island.
 *
 * <p>A self-contained Angular Elements custom element (`<iped-gallery>`)
 * that renders search results as a thumbnail grid. Mirrors the Swing
 * {@code GalleryTable} behaviour: lazy-load thumbnails, blur/gray CSS
 * filters, column-count control, similar-image search trigger.
 *
 * <p>Boundary contract (same as results-grid):
 * <ul>
 *   <li>Inputs: kebab-cased attributes — `case-id`, `search-id`, `api-base`,
 *       `query`, `columns`, `blur-filter`, `gray-filter`.</li>
 *   <li>Outputs: DOM CustomEvents — `item-selected`, `selection-changed`,
 *       `results-loaded`, `similar-image-search`.</li>
 *   <li>Commands in: `previous-page`, `next-page`, `check-all`,
 *       `columns-inc`, `columns-dec`, `blur-toggle`, `gray-toggle`
 *       dispatched as CustomEvents on the host element.</li>
 * </ul>
 */
interface ResultItem {
  itemId: string;
  sourceId?: string;
  score?: number;
  name?: string;
  path?: string;
  mediaType?: string;
  size?: number;
  [k: string]: unknown;
}

interface ResultsPage {
  searchId: string;
  total: number;
  page: {offset: number; limit: number};
  items: ResultItem[];
}

@Component({
  selector: 'iped-gallery-impl',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './gallery.component.scss',
  templateUrl: './gallery.component.html',
})
export class GalleryComponent extends IslandBase implements OnChanges {
  private readonly http = inject(HttpClient);

  @Input('api-base') override apiBase = '/api';
  @Input('case-id') caseId = '';
  @Input('search-id') searchId = '';
  @Input('query') query = '';
  @Input('columns') columns = '5';
  @Input('blur-filter') blurFilter: boolean | string = false;
  @Input('gray-filter') grayFilter: boolean | string = false;

  @HostListener('previous-page') onPreviousPage(): void { this.previousPage(); }
  @HostListener('next-page')     onNextPage(): void     { this.nextPage(); }
  @HostListener('check-all')     onCheckAll(): void     { this.checkAll(); }
  @HostListener('columns-inc')   onColsInc(): void      { this.adjustCols(1); }
  @HostListener('columns-dec')   onColsDec(): void      { this.adjustCols(-1); }

  @HostListener('blur-toggle')
  onBlurToggle(): void {
    this.blurEnabled.update(v => !v);
  }

  @HostListener('gray-toggle')
  onGrayToggle(): void {
    this.grayEnabled.update(v => !v);
  }

  protected readonly items    = signal<ResultItem[]>([]);
  protected readonly total    = signal(0);
  protected readonly offset   = signal(0);
  protected readonly limit    = signal(25);
  protected readonly loading  = signal(false);
  protected readonly error    = signal<string | null>(null);
  protected readonly selectedId = signal<string | null>(null);
  protected readonly checked  = signal<Set<string>>(new Set());
  protected readonly cols     = signal(5);
  protected readonly blurEnabled = signal(false);
  protected readonly grayEnabled = signal(false);

  protected readonly Math = Math;

  ngOnChanges(): void {
    const c = parseInt(this.columns, 10);
    if (!isNaN(c) && c > 0) this.cols.set(Math.min(c, 20));
    this.blurEnabled.set(this.blurFilter === true || this.blurFilter === '' || this.blurFilter === 'true');
    this.grayEnabled.set(this.grayFilter === true || this.grayFilter === '' || this.grayFilter === 'true');
    if (this.caseId) {
      this.searchId = '';
      this.runSearch(0);
    }
  }

  protected filterStyle(): string {
    const parts: string[] = [];
    if (this.blurEnabled()) parts.push('blur(8px)');
    if (this.grayEnabled()) parts.push('grayscale(100%)');
    return parts.join(' ');
  }

  protected runSearch(offset: number): void {
    if (!this.caseId) return;
    this.loading.set(true);
    this.error.set(null);

    const base = this.apiBase.replace(/\/$/, '');

    const fetchResults = (searchId: string) => {
      const url = `${base}/cases/${encodeURIComponent(this.caseId)}/search/${encodeURIComponent(searchId)}/results`;
      this.http.get<ResultsPage>(url, {params: {offset, limit: this.limit()}}).subscribe({
        next: (page) => {
          this.searchId = page.searchId;
          this.items.set(page.items ?? []);
          this.total.set(page.total ?? 0);
          this.offset.set(page.page?.offset ?? offset);
          this.limit.set(page.page?.limit ?? this.limit());
          this.loading.set(false);
          this.dispatch(resultsLoadedEvent({
            total: this.total(),
            shown: this.items().length,
            rangeLabel: this.rangeLabel(),
            hasPrev: this.offset() > 0,
            hasNext: this.offset() + this.limit() < this.total(),
          }));
        },
        error: (e) => this.fail(e),
      });
    };

    if (this.searchId) {
      fetchResults(this.searchId);
    } else {
      const url = `${base}/cases/${encodeURIComponent(this.caseId)}/search`;
      this.http.post<{searchId: string}>(url, {query: this.query || '*'}).subscribe({
        next: (res) => fetchResults(res.searchId),
        error: (e) => this.fail(e),
      });
    }
  }

  protected nextPage(): void {
    if (this.offset() + this.limit() < this.total()) {
      this.runSearch(this.offset() + this.limit());
    }
  }

  protected previousPage(): void {
    if (this.offset() > 0) {
      this.runSearch(Math.max(0, this.offset() - this.limit()));
    }
  }

  protected select(item: ResultItem): void {
    this.selectedId.set(item.itemId);
    this.dispatch(itemSelectedEvent({itemId: item.itemId, sourceId: item.sourceId, name: item.name as string | undefined}));
  }

  protected toggleChecked(item: ResultItem, ev: Event): void {
    ev.stopPropagation();
    const next = new Set(this.checked());
    if (next.has(item.itemId)) next.delete(item.itemId);
    else next.add(item.itemId);
    this.checked.set(next);
    this.dispatch(selectionChangedEvent({count: next.size, itemIds: [...next]}));
  }

  protected checkAll(): void {
    const all = new Set(this.items().map(i => i.itemId));
    this.checked.set(all);
    this.dispatch(selectionChangedEvent({count: all.size, itemIds: [...all]}));
  }

  protected isChecked(item: ResultItem): boolean {
    return this.checked().has(item.itemId);
  }

  protected adjustCols(delta: number): void {
    this.cols.update(c => Math.min(20, Math.max(1, c + delta)));
  }

  /**
   * Thumbnail URL for a real item (sourceId:docId format).
   * Returns null for stub IDs — the template shows a colour placeholder.
   */
  protected thumbUrl(item: ResultItem): string | null {
    if (!item.sourceId) return null;
    // Extract numeric docId from itemId if it follows the "{sourceId}:{docId}" pattern
    const colonIdx = item.itemId.lastIndexOf(':');
    if (colonIdx > 0) {
      const docId = item.itemId.substring(colonIdx + 1);
      if (/^\d+$/.test(docId)) {
        return `${this.apiBase.replace(/\/$/, '')}/sources/${item.sourceId}/docs/${docId}/thumb`;
      }
    }
    return null;
  }

  protected onThumbError(ev: Event): void {
    const img = ev.target as HTMLImageElement;
    img.style.display = 'none';
    const placeholder = img.nextElementSibling as HTMLElement | null;
    if (placeholder) placeholder.style.display = 'flex';
  }

  protected extOf(item: ResultItem): string {
    const name = item.name || item.path || '';
    const dot = name.lastIndexOf('.');
    return dot >= 0 ? name.slice(dot + 1).toLowerCase() : '';
  }

  protected extColor(item: ResultItem): string {
    const ext = this.extOf(item);
    const hue =
      ext === 'pdf'  ? '20'
      : ext === 'xlsx' ? '150'
      : ext === 'docx' ? '235'
      : ext === 'eml'  ? '240'
      : ext === 'jpg' || ext === 'jpeg' || ext === 'png' ? '300'
      : ext === 'mp4' || ext === 'avi'  ? '180'
      : '250';
    return `oklch(0.55 0.13 ${hue})`;
  }

  protected fileName(item: ResultItem): string {
    const p = item.name || item.path || item.itemId;
    const idx = p.lastIndexOf('/');
    return idx >= 0 ? p.slice(idx + 1) : p;
  }

  protected rangeLabel(): string {
    if (this.total() === 0) return '0';
    return `${this.offset() + 1}–${Math.min(this.offset() + this.items().length, this.total())} of ${this.total()}`;
  }

  private fail(e: unknown): void {
    this.loading.set(false);
    this.error.set(e instanceof Error ? e.message : 'Failed to load results');
  }
}
