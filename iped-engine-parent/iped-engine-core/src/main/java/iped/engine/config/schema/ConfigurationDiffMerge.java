/*
 * Copyright 2012-2014, Luis Filipe da Cruz Nassif
 *
 * This file is part of Indexador e Processador de Evidências Digitais (IPED).
 *
 * IPED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * IPED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with IPED.  If not, see <http://www.gnu.org/licenses/>.
 */
package iped.engine.config.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Utilities for comparing and merging configurations.
 * Provides diff detection and conflict-aware merging.
 */
public class ConfigurationDiffMerge {
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Calculate diff between two configurations.
     *
     * @param config1 First configuration
     * @param config2 Second configuration
     * @return Diff with added, removed, and modified fields
     */
    public ConfigurationDiff diff(ObjectNode config1, ObjectNode config2) {
        ConfigurationDiff diff = new ConfigurationDiff();

        // Find modified and removed fields
        Iterator<Map.Entry<String, JsonNode>> fields1 = config1.fields();
        while (fields1.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields1.next();
            String key = entry.getKey();
            JsonNode val1 = entry.getValue();

            if (!config2.has(key)) {
                diff.addRemoved(key, val1);
            } else {
                JsonNode val2 = config2.get(key);
                if (!val1.equals(val2)) {
                    diff.addModified(key, val1, val2);
                }
            }
        }

        // Find added fields
        Iterator<Map.Entry<String, JsonNode>> fields2 = config2.fields();
        while (fields2.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields2.next();
            String key = entry.getKey();
            if (!config1.has(key)) {
                diff.addAdded(key, entry.getValue());
            }
        }

        return diff;
    }

    /**
     * Merge two configurations, with conflict detection.
     *
     * @param baseConfig Base configuration
     * @param config1 First configuration to merge
     * @param config2 Second configuration to merge
     * @return Merge result with conflicts identified
     */
    public MergeResult merge(ObjectNode baseConfig, ObjectNode config1, ObjectNode config2) {
        MergeResult result = new MergeResult();
        ObjectNode merged = mapper.createObjectNode();

        // Start with base config
        Iterator<Map.Entry<String, JsonNode>> baseFields = baseConfig.fields();
        while (baseFields.hasNext()) {
            Map.Entry<String, JsonNode> entry = baseFields.next();
            merged.set(entry.getKey(), entry.getValue());
        }

        // Get diffs
        ConfigurationDiff diff1 = diff(baseConfig, config1);
        ConfigurationDiff diff2 = diff(baseConfig, config2);

        // Apply non-conflicting changes from config1
        for (String key : diff1.getModified().keySet()) {
            if (!diff2.getModified().containsKey(key)) {
                merged.set(key, config1.get(key));
                result.addApplied(key, config1.get(key), "config1");
            } else {
                // Both configs modified same field - conflict
                JsonNode val1 = config1.get(key);
                JsonNode val2 = config2.get(key);
                if (!val1.equals(val2)) {
                    result.addConflict(key, config1.get(key), config2.get(key));
                } else {
                    // Same value, no conflict
                    merged.set(key, val1);
                    result.addApplied(key, val1, "both");
                }
            }
        }

        // Apply non-conflicting changes from config2
        for (String key : diff2.getModified().keySet()) {
            if (!diff1.getModified().containsKey(key)) {
                merged.set(key, config2.get(key));
                result.addApplied(key, config2.get(key), "config2");
            }
        }

        // Handle added fields
        for (String key : diff1.getAdded().keySet()) {
            if (!diff2.getAdded().containsKey(key)) {
                merged.set(key, config1.get(key));
                result.addApplied(key, config1.get(key), "config1");
            } else {
                JsonNode val1 = config1.get(key);
                JsonNode val2 = config2.get(key);
                if (val1.equals(val2)) {
                    merged.set(key, val1);
                    result.addApplied(key, val1, "both");
                } else {
                    result.addConflict(key, val1, val2);
                }
            }
        }

        for (String key : diff2.getAdded().keySet()) {
            if (!diff1.getAdded().containsKey(key) && !merged.has(key)) {
                merged.set(key, config2.get(key));
                result.addApplied(key, config2.get(key), "config2");
            }
        }

        result.setMergedConfig(merged);
        return result;
    }

    /**
     * Configuration diff information.
     */
    public static class ConfigurationDiff {
        private final Map<String, JsonNode> added = new HashMap<>();
        private final Map<String, JsonNode> removed = new HashMap<>();
        private final Map<String, FieldChange> modified = new HashMap<>();

        public void addAdded(String field, JsonNode value) {
            added.put(field, value);
        }

        public void addRemoved(String field, JsonNode value) {
            removed.put(field, value);
        }

        public void addModified(String field, JsonNode oldValue, JsonNode newValue) {
            modified.put(field, new FieldChange(oldValue, newValue));
        }

        public Map<String, JsonNode> getAdded() { return added; }
        public Map<String, JsonNode> getRemoved() { return removed; }
        public Map<String, FieldChange> getModified() { return modified; }

        public int getTotalChanges() {
            return added.size() + removed.size() + modified.size();
        }
    }

    /**
     * Field change information.
     */
    public static class FieldChange {
        public final JsonNode oldValue;
        public final JsonNode newValue;

        public FieldChange(JsonNode oldValue, JsonNode newValue) {
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }

    /**
     * Merge result with conflict information.
     */
    public static class MergeResult {
        private ObjectNode mergedConfig;
        private final Map<String, JsonNode> applied = new HashMap<>();
        private final Map<String, String> appliedFrom = new HashMap<>();
        private final Map<String, ConflictInfo> conflicts = new HashMap<>();

        public void setMergedConfig(ObjectNode config) {
            this.mergedConfig = config;
        }

        public void addApplied(String field, JsonNode value, String source) {
            applied.put(field, value);
            appliedFrom.put(field, source);
        }

        public void addConflict(String field, JsonNode val1, JsonNode val2) {
            conflicts.put(field, new ConflictInfo(val1, val2));
        }

        public ObjectNode getMergedConfig() { return mergedConfig; }
        public Map<String, JsonNode> getApplied() { return applied; }
        public Map<String, String> getAppliedFrom() { return appliedFrom; }
        public Map<String, ConflictInfo> getConflicts() { return conflicts; }

        public boolean hasConflicts() {
            return !conflicts.isEmpty();
        }

        public int getConflictCount() {
            return conflicts.size();
        }
    }

    /**
     * Conflict information.
     */
    public static class ConflictInfo {
        public final JsonNode value1;
        public final JsonNode value2;

        public ConflictInfo(JsonNode value1, JsonNode value2) {
            this.value1 = value1;
            this.value2 = value2;
        }
    }
}
