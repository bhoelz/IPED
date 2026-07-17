package iped.viewers.api;

import iped.data.IItem;
import iped.data.IItemId;
import java.io.File;
import java.util.List;

public interface AttachmentSearcher {

  File getTmpFile(String luceneQuery);

  IItem getItem(String luceneQuery);

  List<IItem> getItems(String luceneQuery);

  void checkItem(String luceneQuery, boolean checked);

  boolean isChecked(String hash);

  String getHash(IItemId itemId);

  void updateSelectionCache();

  String escapeQuery(String query);
}
