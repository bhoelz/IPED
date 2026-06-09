import {HttpClient} from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  EventEmitter,
  inject,
  Input,
  OnChanges,
  Output,
  signal,
} from '@angular/core';

/**
 * Timeline island (`<iped-timeline>`).
 *
 * <p>Renders a histogram of item counts by date bucket (month granularity by
 * default). Drag horizontally to select a date range; releasing emits
 * `time-range-selected` which the SSR bridge translates into a filter chip.
 *
 * <p>Falls back to synthetic demo data when the API is unreachable so the
 * panel is always visually meaningful before EPIC-WEB-03 lands.
 *
 * <p>Boundary contract:
 * <ul>
 *   <li>Input attrs: {@code case-id}, {@code search-id}, {@code api-base}</li>
 *   <li>Output events: {@code time-range-selected} ({start, end} ISO date strings)</li>
 * </ul>
 */

interface Bucket {
  date: string;
  count: number;
}

interface Bar {
  date: string;
  count: number;
  x: number;
  y: number;
  w: number;
  h: number;
  selected: boolean;
}

interface AxisLabel {
  x: number;
  label: string;
}

const SVG_W   = 800;
const CHART_H = 100;
const AXIS_H  = 20;
const SVG_H   = CHART_H + AXIS_H;
const BAR_GAP = 1;

@Component({
  selector: 'iped-timeline-impl',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './timeline.component.html',
  styleUrl: './timeline.component.scss',
})
export class TimelineComponent implements OnChanges {
  private readonly http = inject(HttpClient);

  @Input('api-base') apiBase  = '/api';
  @Input('case-id')  caseId   = '';
  @Input('search-id') searchId = '';

  @Output('time-range-selected')
  timeRangeSelected = new EventEmitter<{start: string; end: string}>();

  protected readonly buckets = signal<Bucket[]>([]);
  protected readonly loading = signal(false);
  protected readonly error   = signal<string | null>(null);
  protected readonly hovDate = signal<string | null>(null);
  protected readonly hovCount = signal<number | null>(null);

  // Drag-select: fractions 0–1 relative to SVG width
  protected readonly dragAnchor  = signal<number | null>(null);
  protected readonly dragCurrent = signal<number | null>(null);

  readonly svgW   = SVG_W;
  readonly svgH   = SVG_H;
  readonly chartH = CHART_H;
  readonly axisH  = AXIS_H;

  protected readonly bars = computed<Bar[]>(() => {
    const b = this.buckets();
    if (!b.length) return [];
    const max = Math.max(...b.map(x => x.count), 1);
    const bw  = Math.max(1, SVG_W / b.length - BAR_GAP);
    const a   = this.dragAnchor();
    const c   = this.dragCurrent();
    const lo  = a !== null && c !== null ? Math.min(a, c) : null;
    const hi  = a !== null && c !== null ? Math.max(a, c) : null;
    return b.map((bucket, i) => {
      const frac = i / b.length;
      const selected = lo !== null && hi !== null && frac >= lo && frac <= hi;
      const h = Math.max(2, Math.round((bucket.count / max) * (CHART_H - 6)));
      return {
        date: bucket.date, count: bucket.count,
        x: (SVG_W / b.length) * i,
        y: CHART_H - h,
        w: bw, h, selected,
      };
    });
  });

  protected readonly axisLabels = computed<AxisLabel[]>(() => {
    const b = this.buckets();
    if (!b.length) return [];
    const step = Math.max(1, Math.ceil(b.length / 10));
    return b
      .filter((_, i) => i % step === 0)
      .map((bucket, _, src) => {
        const i = b.indexOf(bucket);
        return {
          x: (i / b.length + 0.5 / b.length) * SVG_W,
          label: bucket.date.length >= 7 ? bucket.date.slice(0, 7) : bucket.date,
        };
      });
  });

  protected readonly selX = computed(() => {
    const a = this.dragAnchor(), c = this.dragCurrent();
    return a !== null && c !== null ? Math.min(a, c) * SVG_W : 0;
  });

  protected readonly selWidth = computed(() => {
    const a = this.dragAnchor(), c = this.dragCurrent();
    return a !== null && c !== null ? Math.abs(a - c) * SVG_W : 0;
  });

  protected readonly selLabel = computed(() => {
    const a = this.dragAnchor(), c = this.dragCurrent();
    if (a === null || c === null) return '';
    const b  = this.buckets();
    const lo = Math.floor(Math.min(a, c) * b.length);
    const hi = Math.ceil(Math.max(a, c) * b.length) - 1;
    return `${b[Math.max(0, lo)]?.date ?? ''} → ${b[Math.min(b.length - 1, hi)]?.date ?? ''}`;
  });

  ngOnChanges(): void {
    if (this.caseId) this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);
    const base = this.apiBase.replace(/\/$/, '');
    const url  = this.searchId
      ? `${base}/cases/${encodeURIComponent(this.caseId)}/timeline?searchId=${encodeURIComponent(this.searchId)}`
      : `${base}/cases/${encodeURIComponent(this.caseId)}/timeline`;
    this.http.get<{buckets: Bucket[]}>(url).subscribe({
      next: r => { this.buckets.set(r.buckets ?? []); this.loading.set(false); },
      error: () => { this.buckets.set(demoBuckets()); this.loading.set(false); },
    });
  }

  protected startDrag(ev: MouseEvent): void {
    const rect = (ev.currentTarget as SVGElement).getBoundingClientRect();
    this.dragAnchor.set(frac(ev.clientX, rect));
    this.dragCurrent.set(this.dragAnchor());
  }

  protected moveDrag(ev: MouseEvent): void {
    if (this.dragAnchor() === null) return;
    const rect = (ev.currentTarget as SVGElement).getBoundingClientRect();
    this.dragCurrent.set(frac(ev.clientX, rect));
    const bar = this.bars()[Math.floor(this.dragCurrent()! * this.buckets().length)];
    if (bar) { this.hovDate.set(bar.date); this.hovCount.set(bar.count); }
  }

  protected endDrag(): void {
    const a = this.dragAnchor(), c = this.dragCurrent();
    if (a !== null && c !== null && Math.abs(a - c) > 0.005) {
      const b   = this.buckets();
      const lo  = Math.max(0, Math.floor(Math.min(a, c) * b.length));
      const hi  = Math.min(b.length - 1, Math.ceil(Math.max(a, c) * b.length) - 1);
      this.timeRangeSelected.emit({start: b[lo]?.date ?? '', end: b[hi]?.date ?? ''});
    }
    this.dragAnchor.set(null);
    this.dragCurrent.set(null);
  }

  protected hoverBar(bar: Bar): void {
    this.hovDate.set(bar.date);
    this.hovCount.set(bar.count);
  }

  protected leaveBar(): void {
    this.hovDate.set(null);
    this.hovCount.set(null);
  }
}

function frac(clientX: number, rect: DOMRect): number {
  return Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
}

function demoBuckets(): Bucket[] {
  const out: Bucket[] = [];
  for (let m = 0; m < 48; m++) {
    const y = 2020 + Math.floor(m / 12);
    const mo = m % 12 + 1;
    out.push({
      date: `${y}-${String(mo).padStart(2, '0')}`,
      count: Math.round(5 + Math.abs(Math.sin(m * 0.7 + 1.2)) * 80 + Math.sin(m * 0.23) * 15),
    });
  }
  return out;
}
