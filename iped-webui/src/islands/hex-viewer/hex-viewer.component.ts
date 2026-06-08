import {HttpClient, HttpResponse} from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  HostListener,
  inject,
  Input,
  OnChanges,
  signal,
} from '@angular/core';

/**
 * Hex viewer island (`<iped-hex-viewer>`).
 *
 * <p>Fetches raw bytes from the evidence API using HTTP Range requests,
 * renders them as a classic hex dump (offset | hex pairs | ASCII).
 * Page size is 4 096 bytes (256 rows × 16 bytes each).
 *
 * <p>Boundary contract:
 * <ul>
 *   <li>Input attrs: {@code item-id} ("{sourceId}:{docId}"), {@code api-base}</li>
 *   <li>Commands in: {@code previous-page}, {@code next-page} CustomEvents</li>
 * </ul>
 */

const PAGE = 4096;

interface HexRow {
  offsetHex: string;
  hexCols: string[];   // 16 elements, each a 2-char hex string or '  ' for padding
  ascii: string;
  hasSplit: boolean;   // true when this row has the mid-group gap (after byte 7)
}

@Component({
  selector: 'iped-hex-viewer-impl',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './hex-viewer.component.html',
  styleUrl:    './hex-viewer.component.scss',
})
export class HexViewerComponent implements OnChanges {
  private readonly http = inject(HttpClient);

  @Input('item-id')  itemId  = '';
  @Input('api-base') apiBase = '/api';

  protected readonly rows      = signal<HexRow[]>([]);
  protected readonly pageOff   = signal(0);
  protected readonly fileSize  = signal(-1);
  protected readonly loading   = signal(false);
  protected readonly error     = signal<string | null>(null);

  @HostListener('previous-page') onPrev(): void { this.goPage(-1); }
  @HostListener('next-page')     onNext(): void { this.goPage(+1); }

  ngOnChanges(): void {
    this.pageOff.set(0);
    this.fileSize.set(-1);
    this.error.set(null);
    this.load(0);
  }

  protected goPage(delta: number): void {
    const next = Math.max(0, this.pageOff() + delta * PAGE);
    const maxOff = this.fileSize() > 0 ? Math.max(0, this.fileSize() - PAGE) : 0;
    this.load(Math.min(next, maxOff));
  }

  protected hasPrev(): boolean { return this.pageOff() > 0; }
  protected hasNext(): boolean {
    return this.fileSize() < 0 || this.pageOff() + PAGE < this.fileSize();
  }

  protected pageLabel(): string {
    const off = this.pageOff();
    const end = this.fileSize() > 0 ? Math.min(off + PAGE, this.fileSize()) - 1 : off + PAGE - 1;
    const sizeStr = this.fileSize() > 0 ? ` / ${this.fmtSize(this.fileSize())}` : '';
    return `0x${off.toString(16).toUpperCase().padStart(8, '0')}–0x${end.toString(16).toUpperCase().padStart(8, '0')}${sizeStr}`;
  }

  private load(offset: number): void {
    const colon = this.itemId.lastIndexOf(':');
    if (colon <= 0) {
      this.error.set('Invalid item ID — expected "{sourceId}:{docId}"');
      return;
    }
    const src   = this.itemId.substring(0, colon);
    const docId = this.itemId.substring(colon + 1);
    const url   = `${this.apiBase.replace(/\/$/, '')}/sources/${src}/docs/${docId}/content`;

    this.loading.set(true);
    this.error.set(null);

    this.http.get<ArrayBuffer>(url, {
      responseType: 'arraybuffer' as 'json',
      observe: 'response',
      headers: {Range: `bytes=${offset}-${offset + PAGE - 1}`},
    }).subscribe({
      next: (resp: HttpResponse<ArrayBuffer>) => {
        const cr = resp.headers.get('Content-Range');
        if (cr) {
          const m = cr.match(/\/(\d+)$/);
          if (m) this.fileSize.set(parseInt(m[1], 10));
        } else if (resp.status === 200) {
          this.fileSize.set((resp.body as ArrayBuffer).byteLength);
        }
        this.pageOff.set(offset);
        this.rows.set(this.toRows(new Uint8Array(resp.body as ArrayBuffer), offset));
        this.loading.set(false);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(e?.error?.message ?? e?.message ?? 'Failed to fetch content');
      },
    });
  }

  private toRows(bytes: Uint8Array, baseOffset: number): HexRow[] {
    const out: HexRow[] = [];
    for (let i = 0; i < bytes.length; i += 16) {
      const slice  = bytes.slice(i, i + 16);
      const hexCols: string[] = [];
      let ascii = '';
      for (let j = 0; j < 16; j++) {
        if (j < slice.length) {
          hexCols.push(slice[j].toString(16).padStart(2, '0').toUpperCase());
          const c = slice[j];
          ascii += (c >= 0x20 && c < 0x7f) ? String.fromCharCode(c) : '.';
        } else {
          hexCols.push('  ');
          ascii += ' ';
        }
      }
      out.push({
        offsetHex: (baseOffset + i).toString(16).padStart(8, '0').toUpperCase(),
        hexCols,
        ascii,
        hasSplit: true,
      });
    }
    return out;
  }

  private fmtSize(n: number): string {
    if (n >= 1073741824) return (n / 1073741824).toFixed(1) + ' GB';
    if (n >= 1048576)    return (n / 1048576).toFixed(1)    + ' MB';
    if (n >= 1024)       return (n / 1024).toFixed(1)       + ' KB';
    return n + ' B';
  }
}
