/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package iped.data;

/**
 * Notified when the set of selected (checked) items of a case changes.
 */
public interface SelectionListener {

    /**
     * Called when the selection state of a single item changes.
     *
     * @param item  the item whose selection changed
     * @param value {@code true} if the item was selected, {@code false} if
     *              deselected
     */
    void setSelected(IItemId item, boolean value);

    /** Called when the selection is cleared for all items. */
    void clearAll();

    /** Called when all items are selected at once. */
    void selectAll();

}
