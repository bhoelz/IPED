package iped.parsers.ufed.handler;

import iped.data.IItemReader;
import iped.parsers.ufed.model.ContactPhoto;
import iped.properties.ExtraProperties;
import iped.search.IItemSearcher;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ContactPhotoHandler extends BaseModelHandler<ContactPhoto> {

  protected ContactPhotoHandler(ContactPhoto model) {
    super(model, null);
  }

  @Override
  public void doLoadReferences(IItemSearcher searcher) {

    if (model.getPhotoNodeId() != null) {
      String query =
          searcher.escapeQuery(ExtraProperties.UFED_ID) + ":\"" + model.getPhotoNodeId() + "\"";
      List<IItemReader> result = searcher.search(query);
      if (!result.isEmpty()) {
        if (result.size() > 1) {
          log.warn("Found more than 1 contact photo: {}", result);
        }
        IItemReader contactPhoto = result.get(0);
        model.setReferencedFile(contactPhoto);
      }
    }
  }
}
