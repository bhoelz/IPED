package iped.engine.task.osint;

import iped.data.IItem;
import iped.engine.osint.EngineOsintServices;
import iped.engine.task.AbstractTask;
import iped.osint.spi.OsintExecutionMode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;

@Slf4j
public class OsintEnrichmentTask extends AbstractTask {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void process(IItem item) throws Exception {
        try {
            var source = EngineOsintServices.openReadOnlySource(output);
            try {
                var service = EngineOsintServices.forSource(source);
                var results = service.searchItem(
                        item.getId(),
                        Integer.toString(source.getSourceId()),
                        OsintExecutionMode.PROCESSING,
                        List.of(),
                        java.util.Map.of(),
                        item,
                        EngineOsintServices.extractor());
                for (var entry : service.summarize(results).entrySet()) {
                    item.setExtraAttribute(entry.getKey(), entry.getValue());
                }
                item.setExtraAttribute("osint.hits", service.flattenHits(results));
            } finally {
                source.close();
            }
        } catch (IOException e) {
            log.warn("OSINT enrichment failed for item {}", item.getId(), e);
        }
    }

    @Override
    public String getName() {
        return "OSINT Enrichment";
    }
}
