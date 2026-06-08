package iped.webui.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Per-session filter state: the set of named filters the analyst has applied
 * from sidebar clicks (category, bookmark, metadata flags, etc.).
 *
 * <p>Spring's {@code @SessionScope} wraps this in a CGLIB proxy so it can be
 * injected as a singleton-scoped dependency into controllers while each HTTP
 * session gets its own actual instance.
 */
@Component
@SessionScope
public class FilterState implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * A single named filter chip shown in the top bar.
     *
     * @param id    stable key used in DELETE /workspace/filter/{id}
     * @param type  one of "category", "bookmark", "query", "metadata"
     * @param label human-readable display label for the chip
     */
    public record ActiveFilter(String id, String type, String label) {}

    private final Map<String, ActiveFilter> filters = new LinkedHashMap<>();

    public void put(String id, String type, String label) {
        filters.put(id, new ActiveFilter(id, type, label));
    }

    public boolean remove(String id) {
        return filters.remove(id) != null;
    }

    public void clear() {
        filters.clear();
    }

    public List<ActiveFilter> list() {
        return new ArrayList<>(filters.values());
    }

    public boolean hasFilters() {
        return !filters.isEmpty();
    }
}
