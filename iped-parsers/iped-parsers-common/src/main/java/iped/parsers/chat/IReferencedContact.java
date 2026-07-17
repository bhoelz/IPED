package iped.parsers.chat;

import iped.data.IItemReader;

/**
 * Minimal view of a contact referenced from a chat party. Implemented by concrete UFED /
 * chat-framework contact models in parser modules without pulling those heavy types into {@code
 * iped-parsers-common}.
 */
public interface IReferencedContact {

  IItemReader getItem();

  String getUserID();

  String getName();

  String getPhoneNumber();

  String getUsername();
}
