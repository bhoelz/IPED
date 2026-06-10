/*
 * Copyright 2012-2014, Luis Filipe da Cruz Nassif
 *
 * This file is part of Indexador e Processador de EvidÃªncias Digitais (IPED).
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
package iped.engine.config;

import iped.configuration.IConfigurationDirectory;
import iped.utils.TomlProperties;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.NoOpLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

//import java.security.Policy;
//import org.apache.commons.logging.LogFactory;
//import iped.viewers.util.DefaultPolicy;

/**
 * Classe principal de carregamento e acesso às configurações da aplicação.
 */
public class Configuration {

    public static final String CONFIG_FILE = "IPEDConfig.toml"; //$NON-NLS-1$
    public static final String LOCAL_CONFIG = "LocalConfig.toml"; //$NON-NLS-1$
    public static final String CONF_DIR = "conf"; //$NON-NLS-1$
    public static final String PROFILES_DIR = "profiles"; //$NON-NLS-1$
    public static final String CASE_PROFILE_DIR = "profile"; //$NON-NLS-1$

    private static final File ipedRoot = new File(System.getProperty("user.home"), ".iped/ipedRoot.txt");

    private static Configuration singleton;
    private static AtomicBoolean loaded = new AtomicBoolean();

    private ConfigurationDirectory configDirectory;
    public Logger logger;
    public TomlProperties properties = new TomlProperties();
    public String configPath, appRoot;
    public String loaddbPathWin;

    public static Configuration getInstance() {
        if (singleton == null) {
            synchronized (Configuration.class) {
                if (singleton == null)
                    singleton = new Configuration();
            }
        }
        return singleton;
    }

    private Configuration() {
    }

    private String getAppRoot(String configPath) {
        String appRoot = new File(configPath).getAbsolutePath();
        if (appRoot.contains(PROFILES_DIR))
            appRoot = new File(appRoot).getParentFile().getParent();
        return appRoot;
    }

    private void configureLogger(String configPath) {
        // DataSource.testConnection(configPathStr);
        LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log", NoOpLog.class.getName()); //$NON-NLS-1$
        logger = LoggerFactory.getLogger(Configuration.class);
    }

    /**
     * Configurações a partir do caminho informado.
     */
    public void getConfiguration(String configPathStr) throws IOException {

        configPath = configPathStr;

        if (appRoot == null) {
            appRoot = getAppRoot(configPath);
        }

        configureLogger(configPath);

        System.setProperty(IConfigurationDirectory.IPED_APP_ROOT, appRoot);
        System.setProperty(IConfigurationDirectory.IPED_CONF_PATH, configPath);

        // built-in defaults shipped on the classpath come first, then local files
        // (which only need to contain deviations) override them key by key
        loadClasspathDefault(LOCAL_CONFIG);
        loadClasspathDefault(CONFIG_FILE);
        File localConfig = new File(appRoot, LOCAL_CONFIG);
        if (localConfig.exists()) {
            properties.load(localConfig.toPath());
        }
        File mainConfig = new File(configPath, CONFIG_FILE);
        if (mainConfig.exists()) {
            properties.load(mainConfig.toPath());
        }
    }

    private void loadClasspathDefault(String fileName) throws IOException {
        Enumeration<URL> resources = Configuration.class.getClassLoader()
                .getResources(ConfigurationDirectory.DEFAULTS_RESOURCE_DIR + "/" + fileName); //$NON-NLS-1$
        while (resources.hasMoreElements()) {
            try (InputStream is = resources.nextElement().openStream()) {
                properties.load(is);
            }
        }
    }

    public void saveIpedRoot(String path) throws IOException {
        System.setProperty(IConfigurationDirectory.IPED_ROOT, path);
        ipedRoot.getParentFile().mkdirs();
        Files.write(ipedRoot.toPath(), path.getBytes(StandardCharsets.UTF_8));
    }

    public void loadIpedRoot() throws IOException {
        if (ipedRoot.exists()) {
            byte[] bytes = Files.readAllBytes(ipedRoot.toPath());
            String path = new String(bytes, StandardCharsets.UTF_8);
            System.setProperty(IConfigurationDirectory.IPED_ROOT, path);
        }
    }

    public void loadNativeLibs() throws IOException {
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) { //$NON-NLS-1$ //$NON-NLS-2$

            String arch = "x86"; //$NON-NLS-1$
            if (System.getProperty("os.arch").contains("64")) //$NON-NLS-1$ //$NON-NLS-2$
                arch = "x64"; //$NON-NLS-1$

            loaddbPathWin = appRoot + "/tools/tsk/" + arch + "/tsk_loaddb"; //$NON-NLS-1$ //$NON-NLS-2$

            File nativelibs = new File(loaddbPathWin).getParentFile().getParentFile();
            nativelibs = new File(nativelibs, arch);
            loadNatLibs(nativelibs);
        }
    }

    /**
     * Carrega bibliotecas nativas de uma pasta, tentando adivinhar a ordem correta
     *
     * @param libDir
     */
    public static void loadNatLibs(File libDir) {

        if (System.getProperty("os.name").startsWith("Windows")) { //$NON-NLS-1$ //$NON-NLS-2$
            LinkedList<File> libList = new LinkedList<File>();
            for (File file : libDir.listFiles())
                if (file.getName().endsWith(".dll")) //$NON-NLS-1$
                    libList.addFirst(file);

            int fail = 0;
            while (!libList.isEmpty()) {
                File lib = libList.removeLast();
                try {
                    System.load(lib.getAbsolutePath());
                    fail = 0;

                } catch (Throwable t) {
                    libList.addFirst(lib);
                    fail++;
                    if (fail == libList.size())
                        throw t;
                }
            }
        }
    }

    public void loadConfigurables(String configPathStr) throws IOException {
        loadConfigurables(configPathStr, false);
    }

    private void addProfileToConfigDirectory(ConfigurationDirectory configDirectory, File profile) {
        File mainConfig = new File(profile, CONFIG_FILE);
        if (mainConfig.exists()) {
            configDirectory.addPath(mainConfig.toPath());
        }
        File localConfig = new File(profile, LOCAL_CONFIG);
        if (localConfig.exists()) {
            configDirectory.addPath(localConfig.toPath());
        }
        File configDir = new File(profile, CONF_DIR);
        if (configDir.exists()) {
            configDirectory.addPath(configDir.toPath());
        }
    }

    public void loadConfigurables(String configPathStr, boolean loadAll) throws IOException {

        if (loaded.getAndSet(true))
            return;

        getConfiguration(configPathStr);

        if (loadAll) {
            logger.info("Loading configuration from " + configPath); //$NON-NLS-1$
        }

        configDirectory = new ConfigurationDirectory(Paths.get(appRoot, LOCAL_CONFIG));

        // lowest precedence layer: built-in defaults shipped by each module
        configDirectory.addClasspathDefaults(Configuration.class.getClassLoader());

        File defaultProfile = new File(appRoot);
        File currentProfile = new File(configPathStr);
        File caseProfile = new File(appRoot, CASE_PROFILE_DIR);
        addProfileToConfigDirectory(configDirectory, defaultProfile);
        if (!currentProfile.equals(defaultProfile)) {
            addProfileToConfigDirectory(configDirectory, currentProfile);
        } else if (caseProfile.exists()) {
            addProfileToConfigDirectory(configDirectory, caseProfile);
        }

        ConfigurationManager configManager = ConfigurationManager.createInstance(configDirectory);
        LocaleConfig lc = new LocaleConfig();
        configManager.addObject(lc);
        configManager.loadConfig(lc);

        PluginConfig pluginConfig = new PluginConfig();
        configManager.addObject(pluginConfig);
        configManager.loadConfig(pluginConfig);
        addPluginJarsToConfigurationLookup(configDirectory, pluginConfig);

        configManager.addObject(new SplashScreenConfig());

        if (!loadAll) {
            configManager.loadConfigs();
            return;
        }

        loadNativeLibs();

        configManager.addObject(new LocalConfig());
//        configManager.addObject(new OCRConfig());
//        configManager.addObject(new FileSystemConfig());
//        configManager.addObject(new AnalysisConfig());
//        configManager.addObject(new AIFiltersConfig());
//        configManager.addObject(new ProcessingPriorityConfig());
//
//        configManager.addObject(new EnableTaskProperty(FaceRecognitionConfig.enableParam));
//        configManager.addObject(new EnableTaskProperty(AgeEstimationConfig.enableParam));
//
//        TaskInstallerConfig taskConfig = new TaskInstallerConfig();
//        configManager.addObject(taskConfig);
//
//        // must load taskConfig before using it
//        configManager.loadConfig(taskConfig);
//
//        for (AbstractTask task : taskConfig.getNewTaskInstances()) {
//            for (Configurable<?> configurable : task.getConfigurables()) {
//                configManager.addObject(configurable);
//            }
//        }

        configManager.loadConfigs();

        validateConfigurations(configManager);

//        // blocks internet access from html viewers
//        DefaultPolicy policy = new DefaultPolicy();
//        MinIOConfig minIOConfig = configManager.findObject(MinIOConfig.class);
//        if (minIOConfig != null && minIOConfig.isEnabled()) {
//            String host = minIOConfig.getHostAndPort();
//            if (host.startsWith("http://") || host.startsWith("https://")) {
//                host = host.substring(host.indexOf("://") + 3);
//            }
//            policy.addAllowedPermission(new SocketPermission(host, "connect,resolve"));
//        }
//        try {
//            Policy.setPolicy(policy);
//            System.setSecurityManager(new SecurityManager());
//        } catch (UnsupportedOperationException e) {
//            // Policy/SecurityManager APIs were removed in Java 17+.
//            // Internet access from HTML viewers will not be restricted on this JVM.
//            logger.warn("Cannot install security policy (Java 17+): {}", e.getMessage());
//        }
    }

    // add plugin jars to the configuration resource look up engine
    private void addPluginJarsToConfigurationLookup(ConfigurationDirectory configDirectory, PluginConfig pluginConfig) {
        File[] jars = pluginConfig.getPluginJars();
        for (File jar : jars) {
            if (jar.getName().endsWith(".jar")) {
                try {
                    configDirectory.addZip(jar.toPath());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (jar.isDirectory()) {
                configDirectory.addPath(jar.toPath());
            }
        }
    }

    private void validateConfigurations(ConfigurationManager configManager) {
        ConfigurationValidator validator = new ConfigurationValidator();
        ConfigurationValidator.ValidationStats stats = validator.validateAll(configManager);

        if (!stats.allPassed()) {
            logger.warn("Configuration validation failed for: {}", stats.failedComponents);
        } else {
            logger.info("All configurations validated successfully");
        }
    }

}
