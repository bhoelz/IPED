import {Injectable, signal} from '@angular/core';
import {firstValueFrom} from 'rxjs';

import {ItemsService} from '../../../core/api/generated/api/items.service';
import {ItemDetails} from '../../../core/api/generated/model/itemDetails';
import {ItemRelationships} from '../../../core/api/generated/model/itemRelationships';

@Injectable({
  providedIn: 'root'
})
export class ItemFacade {
  readonly selectedItemId = signal<string | null>(null);
  readonly details = signal<ItemDetails | null>(null);
  readonly relationships = signal<ItemRelationships | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor(private readonly itemsService: ItemsService) {}

  async select(caseId: string, itemId: string) {
    const normalizedCaseId = caseId.trim();
    const normalizedItemId = itemId.trim();

    if (!normalizedCaseId || !normalizedItemId) {
      this.error.set('Case ID e item ID são obrigatórios para carregar o item.');
      return false;
    }

    this.loading.set(true);
    this.error.set(null);
    this.selectedItemId.set(normalizedItemId);

    try {
      const [details, relationships] = await Promise.all([
        firstValueFrom(
          this.itemsService.getItem({
            caseId: normalizedCaseId,
            itemId: normalizedItemId
          })
        ),
        firstValueFrom(
          this.itemsService.getItemRelationships({
            caseId: normalizedCaseId,
            itemId: normalizedItemId
          })
        )
      ]);

      this.details.set(details);
      this.relationships.set(relationships);
      return true;
    } catch (error) {
      this.details.set(null);
      this.relationships.set(null);
      this.error.set(this.toMessage(error, 'Falha ao carregar detalhes do item.'));
      return false;
    } finally {
      this.loading.set(false);
    }
  }

  clear() {
    this.selectedItemId.set(null);
    this.details.set(null);
    this.relationships.set(null);
    this.error.set(null);
  }

  private toMessage(error: unknown, fallback: string) {
    if (error instanceof Error && error.message) {
      return error.message;
    }

    return fallback;
  }
}
