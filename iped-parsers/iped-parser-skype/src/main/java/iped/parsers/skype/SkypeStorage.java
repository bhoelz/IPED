package iped.parsers.skype;

import iped.search.IItemSearcher;
import java.io.Closeable;
import java.sql.Connection;
import java.util.Collection;

public interface SkypeStorage extends Closeable {

  void searchMediaCache(IItemSearcher searcher);

  Collection<SkypeConversation> extraiMensagens() throws SkypeParserException;

  Collection<SkypeContact> extraiContatos() throws SkypeParserException;

  Collection<SkypeFileTransfer> extraiTransferencias() throws SkypeParserException;

  String getSkypeName();

  SkypeAccount getAccount();

  Connection getConnection() throws SkypeParserException;
}
