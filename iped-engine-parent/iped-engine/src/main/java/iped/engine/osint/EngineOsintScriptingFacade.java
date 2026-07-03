package iped.engine.osint;

import iped.engine.data.IPEDSource;
import iped.osint.spi.OsintExecutionMode;
import iped.osint.spi.OsintIndicatorType;
import iped.osint.spi.OsintResult;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class EngineOsintScriptingFacade {

    private final File moduleDir;
    private volatile IPEDSource source;

    public EngineOsintScriptingFacade(File moduleDir) {
        this.moduleDir = moduleDir;
    }

    public OsintResult search(String pluginId, String indicatorType, String value, Map<String, Object> options) throws Exception {
        IPEDSource src = source();
        return EngineOsintServices.forSource(src).execute(
                pluginId,
                OsintIndicatorType.valueOf(indicatorType.toUpperCase()),
                value,
                null,
                Integer.toString(src.getSourceId()),
                OsintExecutionMode.SCRIPT,
                options == null ? Map.of() : options);
    }

    public List<OsintResult> searchForItem(int itemId, List<String> pluginIds, Map<String, Object> options) throws Exception {
        IPEDSource src = source();
        var item = src.getItemByID(itemId);
        if (item == null) {
            return List.of();
        }
        return EngineOsintServices.forSource(src).searchItem(
                itemId,
                Integer.toString(src.getSourceId()),
                OsintExecutionMode.SCRIPT,
                pluginIds,
                options == null ? Map.of() : options,
                item,
                EngineOsintServices.extractor());
    }

    public List<Map<String, Object>> listResults(Integer itemId, String pluginId, int limit) throws IOException {
        IPEDSource src = source();
        return EngineOsintServices.forSource(src).listResults(Integer.toString(src.getSourceId()), itemId, pluginId, limit)
                .stream()
                .map(result -> Map.<String, Object>of(
                        "executionId", result.executionId(),
                        "pluginId", result.pluginId(),
                        "indicatorType", result.indicatorType().name(),
                        "indicator", result.normalizedValue(),
                        "status", result.status(),
                        "hits", result.hits().size()))
                .toList();
    }

    private synchronized IPEDSource source() {
        if (source == null) {
            source = EngineOsintServices.openReadOnlySource(moduleDir);
        }
        return source;
    }
}
