package iped.viewers.api.events;

import javax.swing.RowSorter.SortKey;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;
import java.util.List;

public class RowSorterTableDataChange extends TableModelEvent{
    List<? extends SortKey> sortKeys = null;

    public RowSorterTableDataChange(TableModel source) {
        super(source);
    }

    public RowSorterTableDataChange(TableModel source, List<? extends SortKey> sortKeys) {
        super(source);
        this.sortKeys=sortKeys;
    }

    public List<? extends SortKey> getSortKeys() {
        return sortKeys;
    }

    public void setSortKeys(List<? extends SortKey> sortKeys) {
        this.sortKeys = sortKeys;
    }

}
