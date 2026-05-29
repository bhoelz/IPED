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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationDiffMergeTest {
    private ConfigurationDiffMerge diffMerge;
    private ObjectMapper mapper;

    @BeforeEach
    public void setUp() {
        diffMerge = new ConfigurationDiffMerge();
        mapper = new ObjectMapper();
    }

    @Test
    public void testDiffWithIdenticalConfigs() {
        ObjectNode config1 = mapper.createObjectNode();
        config1.put("field1", "value1");
        config1.put("field2", 42);

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("field1", "value1");
        config2.put("field2", 42);

        ConfigurationDiffMerge.ConfigurationDiff diff = diffMerge.diff(config1, config2);

        assertEquals(0, diff.getAdded().size(), "No fields should be added");
        assertEquals(0, diff.getRemoved().size(), "No fields should be removed");
        assertEquals(0, diff.getModified().size(), "No fields should be modified");
        assertEquals(0, diff.getTotalChanges(), "Total changes should be 0");
    }

    @Test
    public void testDiffWithAddedFields() {
        ObjectNode config1 = mapper.createObjectNode();
        config1.put("field1", "value1");

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("field1", "value1");
        config2.put("field2", "value2");

        ConfigurationDiffMerge.ConfigurationDiff diff = diffMerge.diff(config1, config2);

        assertEquals(1, diff.getAdded().size(), "One field should be added");
        assertEquals(0, diff.getRemoved().size(), "No fields should be removed");
        assertEquals(0, diff.getModified().size(), "No fields should be modified");
        assertTrue(diff.getAdded().containsKey("field2"));
    }

    @Test
    public void testDiffWithRemovedFields() {
        ObjectNode config1 = mapper.createObjectNode();
        config1.put("field1", "value1");
        config1.put("field2", "value2");

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("field1", "value1");

        ConfigurationDiffMerge.ConfigurationDiff diff = diffMerge.diff(config1, config2);

        assertEquals(0, diff.getAdded().size(), "No fields should be added");
        assertEquals(1, diff.getRemoved().size(), "One field should be removed");
        assertEquals(0, diff.getModified().size(), "No fields should be modified");
        assertTrue(diff.getRemoved().containsKey("field2"));
    }

    @Test
    public void testDiffWithModifiedFields() {
        ObjectNode config1 = mapper.createObjectNode();
        config1.put("field1", "value1");
        config1.put("field2", 42);

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("field1", "value1");
        config2.put("field2", 100);

        ConfigurationDiffMerge.ConfigurationDiff diff = diffMerge.diff(config1, config2);

        assertEquals(0, diff.getAdded().size(), "No fields should be added");
        assertEquals(0, diff.getRemoved().size(), "No fields should be removed");
        assertEquals(1, diff.getModified().size(), "One field should be modified");
        assertTrue(diff.getModified().containsKey("field2"));

        ConfigurationDiffMerge.FieldChange change = diff.getModified().get("field2");
        assertEquals(42, change.oldValue.asInt());
        assertEquals(100, change.newValue.asInt());
    }

    @Test
    public void testMergeWithoutConflicts() {
        ObjectNode baseConfig = mapper.createObjectNode();
        baseConfig.put("field1", "base");
        baseConfig.put("field2", 1);
        baseConfig.put("field3", "base");

        ObjectNode config1 = mapper.createObjectNode();
        config1.put("field1", "config1");
        config1.put("field2", 1);
        config1.put("field3", "base");

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("field1", "base");
        config2.put("field2", 2);
        config2.put("field3", "config2");

        ConfigurationDiffMerge.MergeResult result = diffMerge.merge(baseConfig, config1, config2);

        assertFalse(result.hasConflicts(), "Should not have conflicts");
        assertEquals(0, result.getConflictCount(), "Conflict count should be 0");

        ObjectNode merged = result.getMergedConfig();
        assertEquals("config1", merged.get("field1").asText());
        assertEquals(2, merged.get("field2").asInt());
        assertEquals("config2", merged.get("field3").asText());
    }

    @Test
    public void testMergeWithConflicts() {
        ObjectNode baseConfig = mapper.createObjectNode();
        baseConfig.put("field1", "base");
        baseConfig.put("field2", 1);

        ObjectNode config1 = mapper.createObjectNode();
        config1.put("field1", "config1");
        config1.put("field2", 2);

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("field1", "config2");
        config2.put("field2", 3);

        ConfigurationDiffMerge.MergeResult result = diffMerge.merge(baseConfig, config1, config2);

        assertTrue(result.hasConflicts(), "Should have conflicts");
        assertEquals(2, result.getConflictCount(), "Should have 2 conflicts");
        assertTrue(result.getConflicts().containsKey("field1"));
        assertTrue(result.getConflicts().containsKey("field2"));
    }

    @Test
    public void testMergeWithSameValuesNoDuplicateConflict() {
        ObjectNode baseConfig = mapper.createObjectNode();
        baseConfig.put("field1", "base");

        ObjectNode config1 = mapper.createObjectNode();
        config1.put("field1", "same");

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("field1", "same");

        ConfigurationDiffMerge.MergeResult result = diffMerge.merge(baseConfig, config1, config2);

        assertFalse(result.hasConflicts(), "Should not have conflicts when both set same value");
        assertEquals("same", result.getMergedConfig().get("field1").asText());
    }

    @Test
    public void testMergeWithAddedFieldsConflict() {
        ObjectNode baseConfig = mapper.createObjectNode();

        ObjectNode config1 = mapper.createObjectNode();
        config1.put("newField", "value1");

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("newField", "value2");

        ConfigurationDiffMerge.MergeResult result = diffMerge.merge(baseConfig, config1, config2);

        assertTrue(result.hasConflicts(), "Should have conflict on added field");
        assertEquals(1, result.getConflictCount());
        assertTrue(result.getConflicts().containsKey("newField"));
    }

    @Test
    public void testMergePreservesBaseConfigValues() {
        ObjectNode baseConfig = mapper.createObjectNode();
        baseConfig.put("unchanged", "base");
        baseConfig.put("modified", 1);

        ObjectNode config1 = mapper.createObjectNode();
        config1.put("unchanged", "base");
        config1.put("modified", 2);

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("unchanged", "base");
        config2.put("modified", 1);

        ConfigurationDiffMerge.MergeResult result = diffMerge.merge(baseConfig, config1, config2);

        assertEquals("base", result.getMergedConfig().get("unchanged").asText());
        assertEquals(2, result.getMergedConfig().get("modified").asInt());
    }

    @Test
    public void testComplexMergeScenario() {
        ObjectNode baseConfig = mapper.createObjectNode();
        baseConfig.put("threads", 4);
        baseConfig.put("memory", 8);
        baseConfig.put("timeout", 30);

        ObjectNode config1 = mapper.createObjectNode();
        config1.put("threads", 8);
        config1.put("memory", 8);
        config1.put("timeout", 30);
        config1.put("debug", true);

        ObjectNode config2 = mapper.createObjectNode();
        config2.put("threads", 4);
        config2.put("memory", 16);
        config2.put("timeout", 60);

        ConfigurationDiffMerge.MergeResult result = diffMerge.merge(baseConfig, config1, config2);

        assertFalse(result.hasConflicts(), "No conflicts expected");

        ObjectNode merged = result.getMergedConfig();
        assertEquals(8, merged.get("threads").asInt());
        assertEquals(16, merged.get("memory").asInt());
        assertEquals(60, merged.get("timeout").asInt());
        assertTrue(merged.get("debug").asBoolean());
    }
}
