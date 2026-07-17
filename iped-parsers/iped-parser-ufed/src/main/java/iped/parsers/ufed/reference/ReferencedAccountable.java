package iped.parsers.ufed.reference;

import static iped.parsers.ufed.util.UfedUtils.readUfedMetadata;

import iped.data.IItemReader;
import iped.parsers.chat.IReferencedContact;

public class ReferencedAccountable extends AbstractReferencedItem implements IReferencedContact {

  public ReferencedAccountable(IItemReader item) {
    super(item);
  }

  public String getName() {
    return readUfedMetadata(item, "Name");
  }

  public String getPhoneNumber() {
    return readUfedMetadata(item, "PhoneNumber");
  }

  public String getUsername() {
    return readUfedMetadata(item, "Username");
  }

  public String getUserID() {
    return readUfedMetadata(item, "UserID");
  }
}
