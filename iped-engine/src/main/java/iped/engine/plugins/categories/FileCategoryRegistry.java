package iped.engine.plugins.categories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Central registry for file category hierarchies.
 *
 * Manages registration of file categories with hierarchical structure,
 * allowing plugins to organize forensic artifacts into logical categories.
 *
 * Thread-safe for concurrent access from multiple workers.
 */
public class FileCategoryRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCategoryRegistry.class);

    /**
     * Root of category hierarchy.
     */
    private final CategoryNode root = new CategoryNode("", "", "", "", "");

    /**
     * All categories by path for fast lookup.
     */
    private final Map<String, CategoryNode> categoryMap = new ConcurrentHashMap<>();

    /**
     * Register a category in the hierarchy.
     *
     * @param path hierarchical path (e.g., "Forensic/Windows/Registry")
     * @param displayName human-readable name
     * @param icon icon identifier
     * @param description category description
     * @param componentId component that registered this
     */
    public void registerCategory(String path, String displayName, String icon,
            String description, String componentId) {

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("path cannot be null or empty");
        }

        if (categoryMap.containsKey(path)) {
            // Update existing
            CategoryNode existing = categoryMap.get(path);
            existing.displayName = displayName;
            existing.icon = icon;
            existing.description = description;
            existing.componentId = componentId;
            LOGGER.debug("Updated category: {}", path);
            return;
        }

        // Create parents if needed
        String[] parts = path.split("/");
        CategoryNode currentNode = root;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String parentPath = String.join("/", Arrays.copyOf(parts, i));
            String nodePath = parentPath.isEmpty() ? part : parentPath + "/" + part;

            if (!currentNode.children.containsKey(part)) {
                CategoryNode newNode = new CategoryNode(nodePath, part, "", "", "");
                currentNode.children.put(part, newNode);
                categoryMap.put(nodePath, newNode);
            }

            currentNode = currentNode.children.get(part);
        }

        // Update final node with provided info
        currentNode.displayName = displayName.isEmpty() ? currentNode.path : displayName;
        currentNode.icon = icon;
        currentNode.description = description;
        currentNode.componentId = componentId;

        LOGGER.debug("Registered category: {} ({})", path, displayName);
    }

    /**
     * Get a category by path.
     *
     * @param path category path
     * @return category node or null
     */
    public CategoryDescriptor getCategory(String path) {
        CategoryNode node = categoryMap.get(path);
        if (node == null) {
            return null;
        }
        return new CategoryDescriptor(node.path, node.displayName, node.icon,
            node.description, node.componentId);
    }

    /**
     * Get all registered categories.
     *
     * @return list of all category paths
     */
    public List<String> getAllCategories() {
        return new ArrayList<>(categoryMap.keySet());
    }

    /**
     * Get root level categories.
     *
     * @return list of root category paths
     */
    public List<String> getRootCategories() {
        return root.children.values().stream()
            .map(n -> n.path)
            .collect(Collectors.toList());
    }

    /**
     * Get immediate subcategories of a path.
     *
     * @param parentPath parent category path
     * @return list of subcategory paths
     */
    public List<String> getSubcategories(String parentPath) {
        CategoryNode parent = categoryMap.get(parentPath);
        if (parent == null) {
            return List.of();
        }

        return parent.children.values().stream()
            .map(n -> n.path)
            .collect(Collectors.toList());
    }

    /**
     * Get leaf categories (categories with no children).
     *
     * @return list of leaf category paths
     */
    public List<String> getLeafCategories() {
        return categoryMap.values().stream()
            .filter(n -> n.children.isEmpty())
            .map(n -> n.path)
            .collect(Collectors.toList());
    }

    /**
     * Get categories at a specific depth in hierarchy.
     *
     * @param depth depth level (1 = root children)
     * @return list of categories at depth
     */
    public List<String> getCategoriesByDepth(int depth) {
        return categoryMap.values().stream()
            .filter(n -> getCategoryDepth(n.path) == depth)
            .map(n -> n.path)
            .collect(Collectors.toList());
    }

    /**
     * Get depth of a category in hierarchy.
     *
     * @param path category path
     * @return depth (1 for root children)
     */
    public int getCategoryDepth(String path) {
        if (path == null || path.isEmpty()) {
            return 0;
        }
        return path.split("/").length;
    }

    /**
     * Get categories registered by a component.
     *
     * @param componentId component ID
     * @return list of category paths
     */
    public List<String> getCategoriesByComponent(String componentId) {
        return categoryMap.values().stream()
            .filter(n -> n.componentId != null && n.componentId.equals(componentId))
            .map(n -> n.path)
            .collect(Collectors.toList());
    }

    /**
     * Get full path from root to category.
     *
     * @param path category path
     * @return list of paths from root to target
     */
    public List<String> getPath(String path) {
        List<String> result = new ArrayList<>();
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            String p = String.join("/", Arrays.copyOf(parts, i + 1));
            result.add(p);
        }
        return result;
    }

    /**
     * Get category information.
     *
     * @param path category path
     * @return category descriptor
     */
    public CategoryDescriptor getCategoryInfo(String path) {
        return getCategory(path);
    }

    /**
     * Get registry statistics.
     *
     * @return map of statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total_categories", categoryMap.size());
        stats.put("root_categories", getRootCategories().size());
        stats.put("leaf_categories", getLeafCategories().size());

        Set<String> components = categoryMap.values().stream()
            .map(n -> n.componentId)
            .filter(c -> c != null && !c.isEmpty())
            .collect(Collectors.toSet());
        stats.put("components", components.size());

        return stats;
    }

    /**
     * Clear all registered categories.
     */
    public void clearRegistry() {
        categoryMap.clear();
        root.children.clear();
        LOGGER.info("Cleared file category registry");
    }

    /**
     * Internal node representing a category in the hierarchy.
     */
    private static class CategoryNode {
        String path;
        String displayName;
        String icon;
        String description;
        String componentId;
        final Map<String, CategoryNode> children = new ConcurrentHashMap<>();

        CategoryNode(String path, String displayName, String icon, String description,
                String componentId) {
            this.path = path;
            this.displayName = displayName;
            this.icon = icon;
            this.description = description;
            this.componentId = componentId;
        }
    }

    /**
     * Public descriptor for a category.
     */
    public record CategoryDescriptor(
        String path,
        String displayName,
        String icon,
        String description,
        String componentId
    ) {
        /**
         * Get the depth of this category.
         */
        public int getDepth() {
            return path.split("/").length;
        }

        /**
         * Get parent path of this category.
         */
        public String getParentPath() {
            String[] parts = path.split("/");
            if (parts.length <= 1) {
                return null;
            }
            return String.join("/", Arrays.copyOf(parts, parts.length - 1));
        }

        /**
         * Get name (last part of path).
         */
        public String getName() {
            String[] parts = path.split("/");
            return parts[parts.length - 1];
        }
    }

    /**
     * Hierarchical representation of categories.
     */
    public record CategoryHierarchy(
        String rootPath,
        List<String> children,
        Map<String, Object> metadata
    ) {}

    @Override
    public String toString() {
        return "FileCategoryRegistry{" +
            "categories=" + categoryMap.size() +
            ", roots=" + getRootCategories().size() +
            '}';
    }
}
