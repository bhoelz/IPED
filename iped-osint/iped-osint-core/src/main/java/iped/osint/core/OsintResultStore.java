package iped.osint.core;

import iped.osint.spi.OsintResult;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface OsintResultStore {

    Optional<OsintResult> findLatestByFingerprint(String pluginId, Integer itemId, String fingerprint) throws IOException;

    void save(OsintResult result) throws IOException;

    Optional<OsintResult> get(String executionId) throws IOException;

    List<OsintResult> list(String sourceId, Integer itemId, String pluginId, int limit) throws IOException;
}
