import {computed, Injectable, signal} from '@angular/core';
import {firstValueFrom} from 'rxjs';

import {SearchService} from '../../../core/api/generated/api/search.service';
import {FacetResult} from '../../../core/api/generated/model/facetResult';
import {FilterClause, FilterClauseOpEnum} from '../../../core/api/generated/model/filterClause';
import {SearchCreateRequest} from '../../../core/api/generated/model/searchCreateRequest';
import {SearchResultsPage} from '../../../core/api/generated/model/searchResultsPage';

const DEFAULT_FACET_FIELDS = ['mediaType', 'sourceId'];

@Injectable({
  providedIn: 'root'
})
export class SearchFacade {
  readonly activeQuery = signal('');
  readonly searchId = signal<string | null>(null);
  readonly resultsPage = signal<SearchResultsPage | null>(null);
  readonly facets = signal<Array<FacetResult>>([]);
  readonly selectedFacetValues = signal<Record<string, Array<string>>>({});
  readonly loading = signal(false);
  readonly facetsLoading = signal(false);
  readonly error = signal<string | null>(null);
  readonly hasResults = computed(() => (this.resultsPage()?.items.length ?? 0) > 0);
  readonly total = computed(() => this.resultsPage()?.total ?? 0);
  readonly items = computed(() => this.resultsPage()?.items ?? []);
  readonly offset = computed(() => this.resultsPage()?.page.offset ?? 0);
  readonly limit = computed(() => this.resultsPage()?.page.limit ?? 25);
  readonly hasPreviousPage = computed(() => this.offset() > 0);
  readonly hasNextPage = computed(() => this.offset() + this.limit() < this.total());
  readonly hasActiveFilters = computed(
    () =>
      Object.values(this.selectedFacetValues()).some((values) => values.length > 0)
  );

  constructor(private readonly searchService: SearchService) {}

  async execute(caseId: string, query: string, offset = 0, limit = 25) {
    const normalizedCaseId = caseId.trim();
    const normalizedQuery = query.trim();

    if (!normalizedCaseId) {
      this.error.set('Abra um caso antes de executar a busca.');
      return;
    }

    if (!normalizedQuery) {
      this.error.set('Informe uma consulta para iniciar a busca.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    try {
      const request: SearchCreateRequest = {
        query: normalizedQuery,
        filters: this.buildFilters(),
        page: {
          offset,
          limit
        }
      };

      const created = await firstValueFrom(
        this.searchService.createSearch({
          caseId: normalizedCaseId,
          searchCreateRequest: request
        })
      );

      const page = await firstValueFrom(
        this.searchService.getSearchResults({
          caseId: normalizedCaseId,
          searchId: created.searchId
        })
      );

      this.activeQuery.set(normalizedQuery);
      this.searchId.set(created.searchId);
      this.resultsPage.set(page);

      await this.loadFacets(normalizedCaseId, created.searchId);
    } catch (error) {
      this.searchId.set(null);
      this.resultsPage.set(null);
      this.facets.set([]);
      this.error.set(this.toMessage(error, 'Falha ao executar a busca.'));
    } finally {
      this.loading.set(false);
    }
  }

  async nextPage(caseId: string) {
    if (!this.hasNextPage()) {
      return;
    }

    await this.execute(caseId, this.activeQuery(), this.offset() + this.limit(), this.limit());
  }

  async previousPage(caseId: string) {
    if (!this.hasPreviousPage()) {
      return;
    }

    await this.execute(
      caseId,
      this.activeQuery(),
      Math.max(0, this.offset() - this.limit()),
      this.limit()
    );
  }

  async toggleFacet(caseId: string, field: string, value: string) {
    const current = this.selectedFacetValues();
    const fieldValues = current[field] ?? [];
    const nextFieldValues = fieldValues.includes(value)
      ? fieldValues.filter((candidate) => candidate !== value)
      : [...fieldValues, value];

    const nextSelection = {
      ...current,
      [field]: nextFieldValues
    };

    if (nextFieldValues.length === 0) {
      delete nextSelection[field];
    }

    this.selectedFacetValues.set(nextSelection);

    if (this.activeQuery()) {
      await this.execute(caseId, this.activeQuery(), 0, this.limit());
    }
  }

  async clearAllFacets(caseId: string) {
    this.selectedFacetValues.set({});

    if (this.activeQuery()) {
      await this.execute(caseId, this.activeQuery(), 0, this.limit());
    }
  }

  clear() {
    this.activeQuery.set('');
    this.searchId.set(null);
    this.resultsPage.set(null);
    this.facets.set([]);
    this.selectedFacetValues.set({});
    this.error.set(null);
  }

  isFacetSelected(field: string, value: string) {
    return (this.selectedFacetValues()[field] ?? []).includes(value);
  }

  activeFiltersSummary() {
    return Object.entries(this.selectedFacetValues()).flatMap(([field, values]) =>
      values.map((value) => ({
        field,
        value
      }))
    );
  }

  private async loadFacets(caseId: string, searchId: string) {
    this.facetsLoading.set(true);

    try {
      const response = await firstValueFrom(
        this.searchService.computeSearchFacets({
          caseId,
          searchId,
          searchFacetsRequest: {
            fields: DEFAULT_FACET_FIELDS
          }
        })
      );

      this.facets.set(response.facets);
    } catch {
      this.facets.set([]);
    } finally {
      this.facetsLoading.set(false);
    }
  }

  private buildFilters(): Array<FilterClause> | undefined {
    const filters: Array<FilterClause> = [];

    for (const [field, values] of Object.entries(this.selectedFacetValues())) {
      if (values.length === 1) {
        filters.push({
          field,
          op: FilterClauseOpEnum.eq,
          value: values[0]
        });
        continue;
      }

      if (values.length > 1) {
        filters.push({
          field,
          op: FilterClauseOpEnum.in,
          value: values
        });
      }
    }

    return filters.length > 0 ? filters : undefined;
  }

  private toMessage(error: unknown, fallback: string) {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    return fallback;
  }
}
