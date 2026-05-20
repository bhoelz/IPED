import { Injectable, computed, signal } from '@angular/core';

export interface CheckedItem {
  readonly itemId: string;
  readonly label: string;
}

@Injectable({
  providedIn: 'root'
})
export class SelectionFacade {
  readonly checkedItems = signal<Record<string, CheckedItem>>({});
  readonly checkedCount = computed(() => Object.keys(this.checkedItems()).length);
  readonly checkedEntries = computed(() => Object.values(this.checkedItems()));
  readonly hasCheckedItems = computed(() => this.checkedCount() > 0);

  toggle(item: CheckedItem) {
    const itemId = item.itemId.trim();

    if (!itemId) {
      return;
    }

    const current = this.checkedItems();

    if (current[itemId]) {
      const next = { ...current };
      delete next[itemId];
      this.checkedItems.set(next);
      return;
    }

    this.checkedItems.set({
      ...current,
      [itemId]: {
        itemId,
        label: item.label.trim() || itemId
      }
    });
  }

  checkMany(items: Array<CheckedItem>) {
    const next = { ...this.checkedItems() };

    for (const item of items) {
      const itemId = item.itemId.trim();

      if (!itemId) {
        continue;
      }

      next[itemId] = {
        itemId,
        label: item.label.trim() || itemId
      };
    }

    this.checkedItems.set(next);
  }

  uncheckMany(itemIds: Array<string>) {
    const next = { ...this.checkedItems() };

    for (const itemId of itemIds) {
      delete next[itemId];
    }

    this.checkedItems.set(next);
  }

  isChecked(itemId: string | null | undefined) {
    return !!itemId && !!this.checkedItems()[itemId];
  }

  clear() {
    this.checkedItems.set({});
  }
}
