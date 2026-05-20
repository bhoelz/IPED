import { Component, computed, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  ExportJobRequestFormatEnum
} from '../../../core/api/generated/model/exportJobRequest';
import { JobStatusStatusEnum } from '../../../core/api/generated/model/jobStatus';
import { WebApiRuntimeConfigService } from '../../../core/config/web-api-runtime-config.service';
import { ItemFacade } from '../../item/data-access/item.facade';
import { JobFacade, TrackedJob } from '../../jobs/data-access/job.facade';
import { SearchFacade } from '../../search/data-access/search.facade';
import { SelectionFacade } from '../../selection/data-access/selection.facade';
import { SessionFacade } from '../../session/data-access/session.facade';
import { ViewerFacade } from '../../viewer/data-access/viewer.facade';

type ExportScope = 'selected-item' | 'checked-items' | 'current-page';

@Component({
  selector: 'iped-workspace-page',
  imports: [ReactiveFormsModule],
  templateUrl: './workspace-page.html',
  styleUrl: './workspace-page.scss'
})
export class WorkspacePage {
  private readonly fb = inject(FormBuilder);

  protected readonly runtimeConfig = inject(WebApiRuntimeConfigService);
  protected readonly sessionFacade = inject(SessionFacade);
  protected readonly searchFacade = inject(SearchFacade);
  protected readonly itemFacade = inject(ItemFacade);
  protected readonly viewerFacade = inject(ViewerFacade);
  protected readonly jobFacade = inject(JobFacade);
  protected readonly selectionFacade = inject(SelectionFacade);
  protected readonly exportFormats = [
    ExportJobRequestFormatEnum.zip,
    ExportJobRequestFormatEnum.csv,
    ExportJobRequestFormatEnum.report
  ];
  protected readonly exportScopes: Array<ExportScope> = [
    'selected-item',
    'checked-items',
    'current-page'
  ];

  protected readonly sessionForm = this.fb.nonNullable.group({
    apiBasePath: [this.runtimeConfig.basePath(), [Validators.required]],
    caseId: ['demo-case', [Validators.required]],
    userId: ['web-analyst']
  });

  protected readonly searchForm = this.fb.nonNullable.group({
    query: ['']
  });
  protected readonly viewerSearchForm = this.fb.nonNullable.group({
    term: ['']
  });
  protected readonly exportForm = this.fb.nonNullable.group({
    scope: this.fb.nonNullable.control<ExportScope>('selected-item', {
      validators: [Validators.required]
    }),
    format: [ExportJobRequestFormatEnum.zip, [Validators.required]]
  });

  protected readonly canSearch = computed(
    () => this.sessionFacade.hasActiveSession() && !this.searchFacade.loading()
  );
  protected readonly visibleResultsEnd = computed(
    () => this.searchFacade.offset() + this.searchFacade.items().length
  );
  protected readonly selectedItemCount = computed(() => (this.itemFacade.selectedItemId() ? 1 : 0));
  protected readonly checkedItemCount = computed(() => this.selectionFacade.checkedCount());
  protected readonly currentPageItemCount = computed(() => this.searchFacade.items().length);
  protected readonly currentPageAllChecked = computed(() => {
    const items = this.searchFacade.items();

    return items.length > 0 && items.every((item) => this.selectionFacade.isChecked(item.itemId));
  });
  protected readonly currentPageCheckedCount = computed(
    () =>
      this.searchFacade.items().filter((item) => this.selectionFacade.isChecked(item.itemId)).length
  );

  protected async openCase() {
    if (this.sessionForm.invalid) {
      this.sessionForm.markAllAsTouched();
      return;
    }

    const value = this.sessionForm.getRawValue();

    this.runtimeConfig.updateBasePath(value.apiBasePath);

    const bootstrapped = await this.sessionFacade.bootstrap(value.caseId, value.userId);

    if (bootstrapped) {
      this.itemFacade.clear();
      this.searchFacade.clear();
      this.viewerFacade.clear();
      this.jobFacade.clear();
      this.selectionFacade.clear();
    }
  }

  protected async executeSearch() {
    const query = this.searchForm.controls.query.value;

    await this.searchFacade.execute(this.sessionFacade.caseId(), query);
  }

  protected async nextPage() {
    await this.searchFacade.nextPage(this.sessionFacade.caseId());
  }

  protected async previousPage() {
    await this.searchFacade.previousPage(this.sessionFacade.caseId());
  }

  protected async selectResult(item: Record<string, unknown>) {
    const itemId = typeof item['itemId'] === 'string' ? item['itemId'] : '';

    if (!itemId) {
      return;
    }

    const selectedRow = this.searchFacade.items().findIndex((candidate) => candidate.itemId === itemId);

    const selected = await this.itemFacade.select(this.sessionFacade.caseId(), itemId);

    if (selected && this.itemFacade.details()) {
      this.viewerSearchForm.reset({
        term: ''
      });
      await this.viewerFacade.open(this.sessionFacade.caseId(), this.itemFacade.details()!, {
        queryId: this.searchFacade.searchId(),
        selectedRow: selectedRow >= 0 ? selectedRow : undefined
      });
    } else {
      this.viewerFacade.clear();
    }
  }

  protected itemLabel(item: Record<string, unknown>) {
    const candidates = ['name', 'path', 'title', 'sourceId', 'itemId'];

    for (const candidate of candidates) {
      const value = item[candidate];

      if (typeof value === 'string' && value.trim()) {
        return value;
      }
    }

    return 'Resultado sem rótulo legível';
  }

  protected itemSecondary(item: Record<string, unknown>) {
    const mimeType =
      typeof item['mimeType'] === 'string'
        ? item['mimeType']
        : typeof item['mediaType'] === 'string'
          ? item['mediaType']
          : null;
    const score = typeof item['score'] === 'number' ? item['score'].toFixed(3) : null;
    const itemId = typeof item['itemId'] === 'string' ? item['itemId'] : null;

    return [mimeType, score ? `score ${score}` : null, itemId].filter(Boolean).join(' · ');
  }

  protected isSelected(item: Record<string, unknown>) {
    return this.itemFacade.selectedItemId() === item['itemId'];
  }

  protected visibleResultsLabel() {
    return `${this.searchFacade.offset() + 1}-${Math.min(
      this.visibleResultsEnd(),
      this.searchFacade.total()
    )}`;
  }

  protected checkedResultsLabel() {
    const checkedCount = this.checkedItemCount();

    if (checkedCount === 0) {
      return 'Nenhum item marcado.';
    }

    return `${checkedCount} item(ns) marcados no conjunto de trabalho.`;
  }

  protected metadataEntries() {
    const metadata = this.itemFacade.details()?.metadata ?? {};

    return Object.entries(metadata).slice(0, 12);
  }

  protected async searchInViewer() {
    const term = this.viewerSearchForm.controls.term.value;

    await this.viewerFacade.search(this.sessionFacade.caseId(), term);
  }

  protected async nextViewerHit() {
    await this.viewerFacade.navigate(this.sessionFacade.caseId(), 'next');
  }

  protected async previousViewerHit() {
    await this.viewerFacade.navigate(this.sessionFacade.caseId(), 'prev');
  }

  protected viewerCanSearch() {
    return this.viewerFacade.session()?.capabilities.search ?? false;
  }

  protected async toggleFacet(field: string, value: string) {
    await this.searchFacade.toggleFacet(this.sessionFacade.caseId(), field, value);
  }

  protected async clearAllFacets() {
    await this.searchFacade.clearAllFacets(this.sessionFacade.caseId());
  }

  protected async startExport() {
    const scope = this.exportForm.controls.scope.value;
    const format = this.exportForm.controls.format.value;
    const itemIds = this.exportItemIds(scope);

    await this.jobFacade.startExport({
      caseId: this.sessionFacade.caseId(),
      itemIds,
      format,
      scopeLabel:
        scope === 'selected-item'
          ? 'Item selecionado'
          : scope === 'checked-items'
            ? 'Itens marcados'
            : 'Pagina atual'
    });
  }

  protected exportScopeHint() {
    const scope = this.exportForm.controls.scope.value;

    if (scope === 'selected-item') {
      return this.selectedItemCount() > 0
        ? 'Vai exportar o item atualmente selecionado no workspace.'
        : 'Selecione um resultado para habilitar a exportacao do item atual.';
    }

    if (scope === 'checked-items') {
      return this.checkedItemCount() > 0
        ? `Vai exportar os ${this.checkedItemCount()} itens marcados no conjunto de trabalho.`
        : 'Marque um ou mais resultados para habilitar a exportacao dos itens checked.';
    }

    return this.currentPageItemCount() > 0
      ? `Vai exportar os ${this.currentPageItemCount()} itens visiveis da pagina atual.`
      : 'Execute uma busca para habilitar a exportacao da pagina atual.';
  }

  protected canExport() {
    const scope = this.exportForm.controls.scope.value;
    const hasCase = this.sessionFacade.hasActiveSession();
    const hasScopeItems = (() => {
      switch (scope) {
        case 'selected-item':
          return this.selectedItemCount() > 0;
        case 'checked-items':
          return this.checkedItemCount() > 0;
        default:
          return this.currentPageItemCount() > 0;
      }
    })();

    return hasCase && hasScopeItems && !this.jobFacade.submitting();
  }

  protected isChecked(item: Record<string, unknown>) {
    return this.selectionFacade.isChecked(
      typeof item['itemId'] === 'string' ? item['itemId'] : null
    );
  }

  protected toggleChecked(item: Record<string, unknown>) {
    const itemId = typeof item['itemId'] === 'string' ? item['itemId'] : '';

    if (!itemId) {
      return;
    }

    this.selectionFacade.toggle({
      itemId,
      label: this.itemLabel(item)
    });
  }

  protected checkVisibleItems() {
    this.selectionFacade.checkMany(
      this.searchFacade.items().map((item) => ({
        itemId: item.itemId,
        label: this.itemLabel(item)
      }))
    );
  }

  protected clearVisibleCheckedItems() {
    this.selectionFacade.uncheckMany(this.searchFacade.items().map((item) => item.itemId));
  }

  protected clearAllCheckedItems() {
    this.selectionFacade.clear();
  }

  protected clearFinishedJobs() {
    this.jobFacade.clearFinished();
  }

  protected jobStatusClass(job: TrackedJob) {
    switch (job.status) {
      case JobStatusStatusEnum.completed:
        return 'job-state-success';
      case JobStatusStatusEnum.failed:
      case JobStatusStatusEnum.cancelled:
        return 'job-state-error';
      default:
        return 'job-state-running';
    }
  }

  protected jobProgressLabel(job: TrackedJob) {
    if (typeof job.progress === 'number') {
      return `${Math.round(job.progress * 100)}%`;
    }

    return job.status;
  }

  private exportItemIds(scope: ExportScope) {
    switch (scope) {
      case 'selected-item':
        return this.itemFacade.selectedItemId() ? [this.itemFacade.selectedItemId()!] : [];
      case 'checked-items':
        return this.selectionFacade.checkedEntries().map((item) => item.itemId);
      default:
        return this.searchFacade.items().map((item) => item.itemId).filter(Boolean);
    }
  }
}
