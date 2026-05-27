import { Component, computed, inject, signal, OnDestroy, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ExportJobRequestFormatEnum } from '../../../core/api/generated/model/exportJobRequest';
import { JobStatusStatusEnum } from '../../../core/api/generated/model/jobStatus';
import { WebApiRuntimeConfigService } from '../../../core/config/web-api-runtime-config.service';
import { ItemFacade } from '../../item/data-access/item.facade';
import { JobFacade, TrackedJob } from '../../jobs/data-access/job.facade';
import { SearchFacade } from '../../search/data-access/search.facade';
import { SelectionFacade } from '../../selection/data-access/selection.facade';
import { SessionFacade } from '../../session/data-access/session.facade';
import { ViewerFacade } from '../../viewer/data-access/viewer.facade';

export type SidebarTab = 'cat' | 'meta' | 'coll' | 'rep';
export type MainView   = 'table' | 'gallery' | 'map' | 'timeline' | 'links';
export type InfoTab    = 'hits' | 'sub' | 'par' | 'dup' | 'ref' | 'refby';
export type ViewerMode = 'hex' | 'text' | 'meta' | 'preview';
type ExportScope = 'selected-item' | 'checked-items' | 'current-page';

export interface CategoryNode {
  id: string;
  label: string;
  kind?: string;
  children?: CategoryNode[];
}

@Component({
  selector: 'iped-workspace-page',
  imports: [ReactiveFormsModule],
  templateUrl: './workspace-page.html',
  styleUrl: './workspace-page.scss'
})
export class WorkspacePage implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);

  protected readonly runtimeConfig   = inject(WebApiRuntimeConfigService);
  protected readonly sessionFacade   = inject(SessionFacade);
  protected readonly searchFacade    = inject(SearchFacade);
  protected readonly itemFacade      = inject(ItemFacade);
  protected readonly viewerFacade    = inject(ViewerFacade);
  protected readonly jobFacade       = inject(JobFacade);
  protected readonly selectionFacade = inject(SelectionFacade);

  // ── Global refs available in templates ───────────────────────────────────
  protected readonly Math = Math;

  // ── UI state ──────────────────────────────────────────────────────────────
  protected readonly sidebarTab  = signal<SidebarTab>('cat');
  protected readonly mainView    = signal<MainView>('table');
  protected readonly infoTab     = signal<InfoTab>('hits');
  protected readonly viewerMode  = signal<ViewerMode>('preview');
  protected readonly activeCat   = signal('all');
  protected readonly openCats    = signal<Record<string, boolean>>({ docs: true, comms: true, media: false, browser: false, fs: false, crypto: false });
  protected readonly showConnect = signal(false);
  protected readonly clockLabel  = signal('--:--:-- UTC');

  private clockInterval?: ReturnType<typeof setInterval>;

  // ── Static sidebar data ───────────────────────────────────────────────────
  protected readonly CATEGORIES: CategoryNode[] = [
    { id: 'docs',    label: 'Documents',         kind: 'group', children: [
      { id: 'doc-pdf',   label: 'PDF' },
      { id: 'doc-word',  label: 'Word' },
      { id: 'doc-xls',   label: 'Spreadsheets' },
      { id: 'doc-txt',   label: 'Plain text' },
    ]},
    { id: 'comms',   label: 'Communications',    kind: 'group', children: [
      { id: 'com-mail',  label: 'Email' },
      { id: 'com-chat',  label: 'Chat' },
      { id: 'com-sms',   label: 'SMS / MMS' },
    ]},
    { id: 'media',   label: 'Media',             kind: 'group', children: [
      { id: 'med-img',   label: 'Images' },
      { id: 'med-vid',   label: 'Video' },
      { id: 'med-aud',   label: 'Audio' },
    ]},
    { id: 'browser', label: 'Browser artifacts', kind: 'group', children: [
      { id: 'br-his',    label: 'History' },
      { id: 'br-dl',     label: 'Downloads' },
      { id: 'br-ck',     label: 'Cookies' },
    ]},
    { id: 'fs',      label: 'Filesystem',        kind: 'group', children: [
      { id: 'fs-del',    label: 'Deleted' },
      { id: 'fs-orph',   label: 'Orphaned' },
      { id: 'fs-carv',   label: 'Carved' },
    ]},
    { id: 'crypto',  label: 'Cryptography',      kind: 'group', children: [
      { id: 'cr-wallet', label: 'Wallets' },
      { id: 'cr-keys',   label: 'Keys & certs' },
    ]},
    { id: 'exec',    label: 'Executables',       kind: 'group' },
  ];

  // ── Forms ─────────────────────────────────────────────────────────────────
  protected readonly exportFormats = [
    ExportJobRequestFormatEnum.zip,
    ExportJobRequestFormatEnum.csv,
    ExportJobRequestFormatEnum.report,
  ];
  protected readonly exportScopes: ExportScope[] = ['selected-item', 'checked-items', 'current-page'];

  protected readonly sessionForm = this.fb.nonNullable.group({
    apiBasePath: [this.runtimeConfig.basePath(), [Validators.required]],
    caseId:      ['demo-case', [Validators.required]],
    userId:      ['web-analyst'],
  });
  protected readonly searchForm = this.fb.nonNullable.group({ query: [''] });
  protected readonly viewerSearchForm = this.fb.nonNullable.group({ term: [''] });
  protected readonly exportForm = this.fb.nonNullable.group({
    scope:  this.fb.nonNullable.control<ExportScope>('selected-item', { validators: [Validators.required] }),
    format: [ExportJobRequestFormatEnum.zip, [Validators.required]],
  });

  // ── Computed ──────────────────────────────────────────────────────────────
  protected readonly canSearch = computed(
    () => this.sessionFacade.hasActiveSession() && !this.searchFacade.loading()
  );
  protected readonly visibleResultsEnd = computed(
    () => this.searchFacade.offset() + this.searchFacade.items().length
  );
  protected readonly checkedItemCount = computed(() => this.selectionFacade.checkedCount());
  protected readonly currentPageItemCount = computed(() => this.searchFacade.items().length);
  protected readonly currentPageAllChecked = computed(() => {
    const items = this.searchFacade.items();
    return items.length > 0 && items.every(i => this.selectionFacade.isChecked(i.itemId));
  });
  protected readonly currentPageCheckedCount = computed(
    () => this.searchFacade.items().filter(i => this.selectionFacade.isChecked(i.itemId)).length
  );
  protected readonly selectedItemCount = computed(() => this.itemFacade.selectedItemId() ? 1 : 0);

  // ── Lifecycle ─────────────────────────────────────────────────────────────
  ngOnInit() {
    this.tick();
    this.clockInterval = setInterval(() => this.tick(), 1000);
  }

  ngOnDestroy() {
    clearInterval(this.clockInterval);
  }

  private tick() {
    const d = new Date();
    const p = (n: number) => String(n).padStart(2, '0');
    this.clockLabel.set(`${p(d.getUTCHours())}:${p(d.getUTCMinutes())}:${p(d.getUTCSeconds())} UTC`);
  }

  // ── Tree helpers ──────────────────────────────────────────────────────────
  protected toggleCat(id: string) {
    const c = this.openCats();
    this.openCats.set({ ...c, [id]: !c[id] });
  }

  // ── Item display helpers ──────────────────────────────────────────────────
  protected itemLabel(item: Record<string, unknown>): string {
    for (const k of ['name', 'path', 'title', 'sourceId', 'itemId']) {
      const v = item[k];
      if (typeof v === 'string' && v.trim()) return v;
    }
    return 'Unlabeled item';
  }

  protected itemName(item: Record<string, unknown>): string {
    const p = (item['name'] as string) || (item['path'] as string) || '';
    const idx = p.lastIndexOf('/');
    return idx >= 0 ? p.slice(idx + 1) : p || String(item['itemId'] ?? '');
  }

  protected extOf(item: Record<string, unknown>): string {
    const name = (item['name'] as string) || (item['path'] as string) || '';
    const idx = name.lastIndexOf('.');
    return idx >= 0 ? name.slice(idx + 1).toLowerCase() : '';
  }

  protected mimeLabel(item: Record<string, unknown>): string {
    return (item['mimeType'] as string) || (item['mediaType'] as string) || '—';
  }

  protected scoreOf(item: Record<string, unknown>): number {
    const s = item['score'];
    if (typeof s === 'number') return s <= 1 ? Math.round(s * 100) : Math.round(s);
    return 0;
  }

  protected fmtSize(n: unknown): string {
    const b = typeof n === 'number' ? n : 0;
    if (b < 1024)         return b + ' B';
    if (b < 1_048_576)    return (b / 1024).toFixed(1) + ' KB';
    if (b < 1_073_741_824) return (b / 1_048_576).toFixed(1) + ' MB';
    return (b / 1_073_741_824).toFixed(2) + ' GB';
  }

  protected isSelected(item: Record<string, unknown>): boolean {
    return this.itemFacade.selectedItemId() === item['itemId'];
  }

  protected isChecked(item: Record<string, unknown>): boolean {
    return this.selectionFacade.isChecked(typeof item['itemId'] === 'string' ? item['itemId'] : null);
  }

  protected rowIndex(item: Record<string, unknown>): number {
    return this.searchFacade.items().indexOf(item as never) + 1 + this.searchFacade.offset();
  }

  protected visibleResultsLabel(): string {
    return `${this.searchFacade.offset() + 1}–${Math.min(this.visibleResultsEnd(), this.searchFacade.total())}`;
  }

  protected metadataEntries(): [string, unknown][] {
    return Object.entries(this.itemFacade.details()?.metadata ?? {}).slice(0, 50);
  }

  // ── Actions ───────────────────────────────────────────────────────────────
  protected async openCase() {
    if (this.sessionForm.invalid) { this.sessionForm.markAllAsTouched(); return; }
    const { apiBasePath, caseId, userId } = this.sessionForm.getRawValue();
    this.runtimeConfig.updateBasePath(apiBasePath);
    const ok = await this.sessionFacade.bootstrap(caseId, userId);
    if (ok) {
      [this.itemFacade, this.searchFacade, this.viewerFacade, this.jobFacade, this.selectionFacade]
        .forEach(f => f.clear());
      this.showConnect.set(false);
    }
  }

  protected async executeSearch() {
    await this.searchFacade.execute(this.sessionFacade.caseId(), this.searchForm.controls.query.value);
  }

  protected async nextPage()     { await this.searchFacade.nextPage(this.sessionFacade.caseId()); }
  protected async previousPage() { await this.searchFacade.previousPage(this.sessionFacade.caseId()); }

  protected async selectResult(item: Record<string, unknown>) {
    const itemId = typeof item['itemId'] === 'string' ? item['itemId'] : '';
    if (!itemId) return;
    const selectedRow = this.searchFacade.items().findIndex(c => c.itemId === itemId);
    const selected = await this.itemFacade.select(this.sessionFacade.caseId(), itemId);
    if (selected && this.itemFacade.details()) {
      this.viewerSearchForm.reset({ term: '' });
      await this.viewerFacade.open(this.sessionFacade.caseId(), this.itemFacade.details()!, {
        queryId: this.searchFacade.searchId(),
        selectedRow: selectedRow >= 0 ? selectedRow : undefined,
      });
    } else {
      this.viewerFacade.clear();
    }
  }

  protected toggleChecked(item: Record<string, unknown>) {
    const itemId = typeof item['itemId'] === 'string' ? item['itemId'] : '';
    if (!itemId) return;
    this.selectionFacade.toggle({ itemId, label: this.itemLabel(item) });
  }

  protected checkVisibleItems() {
    this.selectionFacade.checkMany(
      this.searchFacade.items().map(i => ({ itemId: i.itemId, label: this.itemLabel(i) }))
    );
  }
  protected clearVisibleCheckedItems() {
    this.selectionFacade.uncheckMany(this.searchFacade.items().map(i => i.itemId));
  }
  protected clearAllCheckedItems() { this.selectionFacade.clear(); }

  protected async toggleFacet(field: string, value: string) {
    await this.searchFacade.toggleFacet(this.sessionFacade.caseId(), field, value);
  }
  protected async clearAllFacets() {
    await this.searchFacade.clearAllFacets(this.sessionFacade.caseId());
  }

  protected async searchInViewer() {
    await this.viewerFacade.search(this.sessionFacade.caseId(), this.viewerSearchForm.controls.term.value);
  }
  protected async nextViewerHit()     { await this.viewerFacade.navigate(this.sessionFacade.caseId(), 'next'); }
  protected async previousViewerHit() { await this.viewerFacade.navigate(this.sessionFacade.caseId(), 'prev'); }
  protected viewerCanSearch(): boolean { return this.viewerFacade.session()?.capabilities.search ?? false; }

  protected async startExport() {
    const scope  = this.exportForm.controls.scope.value;
    const format = this.exportForm.controls.format.value;
    await this.jobFacade.startExport({
      caseId:     this.sessionFacade.caseId(),
      itemIds:    this.exportItemIds(scope),
      format,
      scopeLabel: scope === 'selected-item' ? 'Selected item'
                : scope === 'checked-items'  ? 'Checked items' : 'Current page',
    });
  }

  protected canExport(): boolean {
    const scope = this.exportForm.controls.scope.value;
    const hasItems = scope === 'selected-item' ? this.selectedItemCount() > 0
                   : scope === 'checked-items'  ? this.checkedItemCount() > 0
                   : this.currentPageItemCount() > 0;
    return this.sessionFacade.hasActiveSession() && hasItems && !this.jobFacade.submitting();
  }

  protected clearFinishedJobs() { this.jobFacade.clearFinished(); }

  protected jobStatusClass(job: TrackedJob): string {
    switch (job.status) {
      case JobStatusStatusEnum.completed: return 'job-ok';
      case JobStatusStatusEnum.failed:
      case JobStatusStatusEnum.cancelled:  return 'job-bad';
      default:                             return 'job-run';
    }
  }

  protected jobProgressLabel(job: TrackedJob): string {
    if (typeof job.progress === 'number') return `${Math.round(job.progress * 100)}%`;
    return job.status;
  }

  private exportItemIds(scope: ExportScope): string[] {
    switch (scope) {
      case 'selected-item': return this.itemFacade.selectedItemId() ? [this.itemFacade.selectedItemId()!] : [];
      case 'checked-items': return this.selectionFacade.checkedEntries().map(i => i.itemId);
      default:              return this.searchFacade.items().map(i => i.itemId).filter(Boolean);
    }
  }
}
