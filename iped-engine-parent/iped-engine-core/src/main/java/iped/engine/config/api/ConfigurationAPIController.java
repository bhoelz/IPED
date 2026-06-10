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
package iped.engine.config.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import iped.configuration.Configurable;
import iped.engine.config.ConfigurationManager;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * API controller for configuration management.
 * Provides REST endpoints for managing configurations.
 */
@Slf4j
public class ConfigurationAPIController {
    private final ObjectMapper mapper = new ObjectMapper();
    private final ConfigurationManager configManager;
    private final Map<String, String> configurationStore = new HashMap<>();

    public ConfigurationAPIController(ConfigurationManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Get current configuration state.
     *
     * @return Current configurations
     */
    public Map<String, Object> getCurrentConfiguration() {
        try {
            Map<String, Object> configs = new HashMap<>();
            Set<Configurable<?>> loadedConfigs = configManager.getObjects();

            for (Configurable<?> config : loadedConfigs) {
                String configName = config.getClass().getSimpleName();
                configs.put(configName, mapper.convertValue(config, Object.class));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configurations", configs);
            response.put("count", configs.size());
            return response;
        } catch (Exception e) {
            log.error("Error getting current configuration", e);
            return errorResponse("Error getting configuration: " + e.getMessage());
        }
    }

    /**
     * Get a specific configuration.
     *
     * @param componentName Name of the configuration component
     * @return The configuration
     */
    public Map<String, Object> getConfiguration(String componentName) {
        try {
            Set<Configurable<?>> configs = configManager.findObjects(componentName);

            if (configs.isEmpty()) {
                return errorResponse("Configuration not found: " + componentName);
            }

            Configurable<?> config = configs.iterator().next();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("name", componentName);
            response.put("data", mapper.convertValue(config, Object.class));
            return response;
        } catch (Exception e) {
            log.error("Error getting configuration: " + componentName, e);
            return errorResponse("Error getting configuration: " + e.getMessage());
        }
    }

    /**
     * Export configuration to JSON.
     *
     * @param componentName Configuration component name
     * @return Configuration as JSON string
     */
    public Map<String, Object> exportConfiguration(String componentName) {
        try {
            Map<String, Object> configData = getConfiguration(componentName);
            if (!(boolean) configData.get("success")) {
                return configData;
            }

            String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(configData.get("data"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("componentName", componentName);
            response.put("json", json);
            response.put("timestamp", System.currentTimeMillis());
            return response;
        } catch (Exception e) {
            log.error("Error exporting configuration", e);
            return errorResponse("Error exporting configuration: " + e.getMessage());
        }
    }

    /**
     * Export all configurations.
     *
     * @return All configurations as JSON
     */
    public Map<String, Object> exportAllConfigurations() {
        try {
            Map<String, Object> allConfigs = new HashMap<>();
            Set<Configurable<?>> configs = configManager.getObjects();

            for (Configurable<?> config : configs) {
                String name = config.getClass().getSimpleName();
                allConfigs.put(name, mapper.convertValue(config, Object.class));
            }

            String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(allConfigs);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("json", json);
            response.put("count", allConfigs.size());
            response.put("timestamp", System.currentTimeMillis());
            return response;
        } catch (Exception e) {
            log.error("Error exporting all configurations", e);
            return errorResponse("Error exporting configurations: " + e.getMessage());
        }
    }

    /**
     * Save configuration to file.
     *
     * @param componentName Configuration component name
     * @param outputPath Path to save configuration
     * @return Result status
     */
    public Map<String, Object> saveConfiguration(String componentName, String outputPath) {
        try {
            Map<String, Object> exported = exportConfiguration(componentName);
            if (!(boolean) exported.get("success")) {
                return exported;
            }

            String json = (String) exported.get("json");
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());
            Files.write(path, json.getBytes());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("componentName", componentName);
            response.put("savedTo", outputPath);
            response.put("size", Files.size(path));
            return response;
        } catch (Exception e) {
            log.error("Error saving configuration", e);
            return errorResponse("Error saving configuration: " + e.getMessage());
        }
    }

    /**
     * Get configuration metadata.
     *
     * @return Metadata about all configurations
     */
    public Map<String, Object> getConfigurationMetadata() {
        try {
            List<Map<String, Object>> metadata = new ArrayList<>();
            Set<Configurable<?>> configs = configManager.getObjects();

            for (Configurable<?> config : configs) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", config.getClass().getSimpleName());
                item.put("className", config.getClass().getCanonicalName());
                item.put("packageName", config.getClass().getPackageName());
                item.put("timestamp", System.currentTimeMillis());
                metadata.add(item);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("configurations", metadata);
            response.put("count", metadata.size());
            return response;
        } catch (Exception e) {
            log.error("Error getting configuration metadata", e);
            return errorResponse("Error getting metadata: " + e.getMessage());
        }
    }

    /**
     * Create a configuration backup.
     *
     * @param backupPath Path to save backup
     * @return Backup status
     */
    public Map<String, Object> createBackup(String backupPath) {
        try {
            String backupId = UUID.randomUUID().toString();
            Map<String, Object> allConfigs = (Map<String, Object>) exportAllConfigurations().get("json");

            Path path = Paths.get(backupPath, backupId + ".json");
            Files.createDirectories(path.getParent());

            String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(allConfigs);
            Files.write(path, json.getBytes());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("backupId", backupId);
            response.put("backupPath", path.toString());
            response.put("timestamp", System.currentTimeMillis());
            return response;
        } catch (Exception e) {
            log.error("Error creating backup", e);
            return errorResponse("Error creating backup: " + e.getMessage());
        }
    }

    /**
     * Create error response.
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("error", message);
        return response;
    }
}
