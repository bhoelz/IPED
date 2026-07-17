package iped.engine.config.registry;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

final class RegistryJson {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

  private RegistryJson() {}

  static RegistryIndex parseIndex(String json) throws IOException {
    return MAPPER.readValue(json, RegistryIndex.class);
  }
}
