package iped.parsers.mail.win10.entries;

public abstract class AbstractEntry {
  int rowId;

  public AbstractEntry(int rowId) {
    this.rowId = rowId;
  }

  public int getRowId() {
    return this.rowId;
  }

  public void setRowId(int rowId) {
    this.rowId = rowId;
  }
}
