import {HttpClient} from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnChanges,
  Output,
  signal,
} from '@angular/core';

/**
 * Results-grid island.
 *
 * <p>A self-contained Angular Elements custom element (`<iped-results-grid>`)
 * that owns its own DOM, local UI state, and JSON calls to `/api`. It is the
 * rich-interaction surface of the workspace page; the surrounding SSR shell
 * never patches inside it.
 *
 * <p>Boundary contract:
 * <ul>
 *   <li>Inputs arrive as kebab-cased attributes: `case-id`, `search-id`, `api-base`.</li>
 *   <li>Outputs leave as DOM CustomEvents: `item-selected`, `selection-changed`.</li>
 * </ul>
 * No shared client state with HTMX — communication is attributes in, events out.
 */
interface ResultItem {
  itemId: string;
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
  selector: 'iped-results-grid-impl',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './results-grid.component.scss',
  templateUrl: './results-grid.component.html',
})
export class ResultsGridComponent implements OnChanges {
  private readonly http = inject(HttpClient);

  @Input('api-base') apiBase = '/api';
  @Input('case-id') caseId = '';
  @Input('search-id') searchId = '';
  @Input('query') query = '';

  /** Emitted as the `item-selected` DOM CustomEvent when a row is selected. */
  @Output('item-selected') itemSelected = new EventEmitter<{itemId: string}>();
  /** Emitted as the `selection-changed` DOM CustomEvent when checked set changes. */
  @Output('selection-changed') selectionChanged = new EventEmitter<{count: number; itemIds: string[]}>();
  /** Emitted as the `results-loaded` DOM CustomEvent after a page of results loads. */
  @Output('results-loaded') resultsLoaded = new EventEmitter<{
    total: number;
    shown: number;
    rangeLabel: string;
    hasPrev: boolean;
    hasNext: boolean;
  }>();

  // Paging is chrome owned by the SSR panel header; it commands the island via
  // DOM CustomEvents dispatched on this host (data ownership stays in the island).
  @HostListener('previous-page')
  onPreviousPage(): void {
    this.previousPage();
  }

  @HostListener('next-page')
  onNextPage(): void {
    this.nextPage();
  }

  protected readonly items = signal<ResultItem[]>([]);
  protected readonly total = signal(0);
  protected readonly offset = signal(0);
  protected readonly limit = signal(25);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly selectedId = signal<string | null>(null);
  protected readonly checked = signal<Set<string>>(new Set());

  protected readonly Math = Math;

  ngOnChanges(): void {
    if (this.caseId) {
      // An input (case-id / query) changed → start a fresh search from page 0.
      this.searchId = '';
      this.runSearch(0);
    }
  }

  protected runSearch(offset: number): void {
    if (!this.caseId) {
      return;
    }
    this.loading.set(true);
    this.error.set(null);

    const base = this.apiBase.replace(/\/$/, '');
    const fetchResults = (searchId: string) => {
      const url = `${base}/cases/${encodeURIComponent(this.caseId)}/search/${encodeURIComponent(searchId)}/results`;
      this.http
        .get<ResultsPage>(url, {params: {offset, limit: this.limit()}})
        .subscribe({
          next: (page) => {
            this.searchId = page.searchId;
            this.items.set(page.items ?? []);
            this.total.set(page.total ?? 0);
            this.offset.set(page.page?.offset ?? offset);
            this.limit.set(page.page?.limit ?? this.limit());
            this.loading.set(false);
            this.resultsLoaded.emit({
              total: this.total(),
              shown: this.items().length,
              rangeLabel: this.rangeLabel(),
              hasPrev: this.offset() > 0,
              hasNext: this.offset() + this.limit() < this.total(),
            });
          },
          error: (e) => this.fail(e),
        });
    };

    if (this.searchId) {
      fetchResults(this.searchId);
    } else {
      const url = `${base}/cases/${encodeURIComponent(this.caseId)}/search`;
      this.http.post<{searchId: string}>(url, {query: this.query || '*'}).subscribe({
        next: (created) => fetchResults(created.searchId),
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
    this.itemSelected.emit({itemId: item.itemId});
  }

  protected toggleChecked(item: ResultItem, ev: Event): void {
    ev.stopPropagation();
    const next = new Set(this.checked());
    if (next.has(item.itemId)) {
      next.delete(item.itemId);
    } else {
      next.add(item.itemId);
    }
    this.checked.set(next);
    this.selectionChanged.emit({count: next.size, itemIds: [...next]});
  }

  protected isChecked(item: ResultItem): boolean {
    return this.checked().has(item.itemId);
  }

  protected scorePct(item: ResultItem): number {
    const s = item.score ?? 0;
    return s <= 1 ? Math.round(s * 100) : Math.round(s);
  }

  protected fileName(item: ResultItem): string {
    const p = item.name || item.path || item.itemId;
    const idx = p.lastIndexOf('/');
    return idx >= 0 ? p.slice(idx + 1) : p;
  }

  private extOf(item: ResultItem): string {
    const name = item.name || item.path || '';
    const idx = name.lastIndexOf('.');
    return idx >= 0 ? name.slice(idx + 1).toLowerCase() : '';
  }

  /** Per-extension dot colour, matching the SPA results table. */
  protected extColor(item: ResultItem): string {
    const ext = this.extOf(item);
    const hue =
      ext === 'pdf' ? '20'
      : ext === 'xlsx' ? '150'
      : ext === 'docx' ? '235'
      : ext === 'eml' ? '240'
      : ext === 'jpg' || ext === 'png' ? '300'
      : '250';
    return `oklch(0.72 0.12 ${hue})`;
  }

  protected fmtSize(n: unknown): string {
    const b = typeof n === 'number' ? n : 0;
    if (b < 1024) return `${b} B`;
    if (b < 1_048_576) return `${(b / 1024).toFixed(1)} KB`;
    if (b < 1_073_741_824) return `${(b / 1_048_576).toFixed(1)} MB`;
    return `${(b / 1_073_741_824).toFixed(2)} GB`;
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
