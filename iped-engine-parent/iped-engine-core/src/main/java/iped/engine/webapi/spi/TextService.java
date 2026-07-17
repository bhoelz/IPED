package iped.engine.webapi.spi;

import java.io.OutputStream;

public interface TextService {
  void writeText(String sourceId, int id, OutputStream output) throws Exception;
}
