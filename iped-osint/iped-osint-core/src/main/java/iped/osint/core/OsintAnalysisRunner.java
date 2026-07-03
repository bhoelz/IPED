package iped.osint.core;

import iped.data.IIPEDSource;
import iped.data.IItemId;
import iped.osint.spi.OsintExecutionMode;
import iped.osint.spi.OsintResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class OsintAnalysisRunner {

    private final IIPEDSource source;
    private final OsintService osintService;
    private final OsintIndicatorExtractor extractor;

    public OsintAnalysisRunner(IIPEDSource source, OsintService osintService, OsintIndicatorExtractor extractor) {
        this.source = source;
        this.osintService = osintService;
        this.extractor = extractor;
    }

    public CompletableFuture<List<OsintResult>> run(Collection<? extends IItemId> itemIds, List<String> pluginIds,
                                                    Consumer<OsintAnalysisProgress> callback) {
        return CompletableFuture.supplyAsync(() -> {
            List<OsintResult> results = new ArrayList<>();
            int total = itemIds.size();
            AtomicInteger processed = new AtomicInteger();
            AtomicInteger errors = new AtomicInteger();
            for (IItemId itemId : itemIds) {
                try {
                    var item = source.getItemByID(itemId.getId());
                    if (item != null) {
                        results.addAll(osintService.searchItem(itemId.getId(), Integer.toString(source.getSourceId()),
                                OsintExecutionMode.ANALYSIS, pluginIds, java.util.Map.of(), item, extractor));
                    }
                } catch (IOException e) {
                    errors.incrementAndGet();
                } finally {
                    int done = processed.incrementAndGet();
                    if (callback != null) {
                        callback.accept(new OsintAnalysisProgress(done, total, errors.get()));
                    }
                }
            }
            return results;
        });
    }
}
