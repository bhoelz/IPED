package iped.utils;

import iped.io.ISeekableInputStreamFactory;
import iped.io.SeekableInputStream;
import java.io.IOException;
import java.net.URI;

public abstract class SeekableInputStreamFactory implements ISeekableInputStreamFactory {

  protected URI dataSource;

  public SeekableInputStreamFactory(URI dataSource) {
    this.dataSource = dataSource;
  }

  public boolean checkIfDataSourceExists() {
    return true;
  }

  public abstract SeekableInputStream getSeekableInputStream(String identifier) throws IOException;

  public URI getDataSourceURI() {
    return dataSource;
  }

  public void setDataSourceURI(URI dataSource) {
    this.dataSource = dataSource;
  }
}
