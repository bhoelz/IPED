import {TestBed} from '@angular/core/testing';

import {SelectionFacade} from './selection.facade';

describe('SelectionFacade', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SelectionFacade]
    });
  });

  it('should toggle checked items on and off', () => {
    const facade = TestBed.inject(SelectionFacade);

    facade.toggle({
      itemId: 'item-1',
      label: 'Primeiro item'
    });

    expect(facade.checkedCount()).toBe(1);
    expect(facade.isChecked('item-1')).toBe(true);

    facade.toggle({
      itemId: 'item-1',
      label: 'Primeiro item'
    });

    expect(facade.checkedCount()).toBe(0);
    expect(facade.isChecked('item-1')).toBe(false);
  });

  it('should keep checked items unique across batch operations', () => {
    const facade = TestBed.inject(SelectionFacade);

    facade.checkMany([
      { itemId: 'item-1', label: 'Um' },
      { itemId: 'item-2', label: 'Dois' }
    ]);
    facade.checkMany([
      { itemId: 'item-2', label: 'Dois atualizado' },
      { itemId: 'item-3', label: 'Tres' }
    ]);

    expect(facade.checkedEntries()).toEqual([
      { itemId: 'item-1', label: 'Um' },
      { itemId: 'item-2', label: 'Dois atualizado' },
      { itemId: 'item-3', label: 'Tres' }
    ]);

    facade.uncheckMany(['item-1', 'item-3']);

    expect(facade.checkedEntries()).toEqual([{ itemId: 'item-2', label: 'Dois atualizado' }]);
  });
});
