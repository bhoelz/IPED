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

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import iped.engine.config.Configuration;

/**
 * Embedded Jetty server for Configuration API.
 * Starts JAX-RS/Jersey service on configurable port.
 */
public class ConfigurationServer {
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationServer.class);
    private Server server;
    private int port;

    /**
     * Create configuration API server on specified port.
     *
     * @param port HTTP server port
     */
    public ConfigurationServer(int port) {
        this.port = port;
    }

    /**
     * Start the embedded Jetty server.
     *
     * @throws Exception if server startup fails
     */
    public void start() throws Exception {
        // Initialize Configuration Manager to load schemas
        try {
            Configuration.getInstance();
            logger.info("Configuration Manager initialized successfully");
        } catch (NoClassDefFoundError e) {
            // Viewer/UI classes not available in API-only mode
            logger.info("Running in API-only mode without viewer support: {}", e.getMessage());
        } catch (Exception e) {
            logger.warn("Configuration Manager initialization warning: {}", e.getMessage());
        }

        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);

        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer());
        jerseyServlet.setInitParameter("jersey.config.server.provider.packages",
                "iped.engine.config.api");
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                "org.glassfish.jersey.media.json.JsonProcessingFeature,com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider");

        context.addServlet(jerseyServlet, "/*");

        server.start();
        logger.info("Configuration API server started on port {}", port);
    }

    /**
     * Stop the embedded Jetty server.
     *
     * @throws Exception if server shutdown fails
     */
    public void stop() throws Exception {
        if (server != null) {
            server.stop();
            logger.info("Configuration API server stopped");
        }
    }

    /**
     * Wait for server to complete (blocking call).
     *
     * @throws Exception if join fails
     */
    public void join() throws Exception {
        if (server != null) {
            server.join();
        }
    }

    /**
     * Check if server is running.
     *
     * @return true if server is started and running
     */
    public boolean isRunning() {
        return server != null && server.isRunning();
    }

    /**
     * Get server port.
     *
     * @return HTTP port
     */
    public int getPort() {
        return port;
    }

    /**
     * Main entry point for standalone server.
     *
     * @param args command line arguments (optional port as first argument)
     */
    public static void main(String[] args) {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port: " + args[0]);
                System.exit(1);
            }
        }

        try {
            ConfigurationServer server = new ConfigurationServer(port);
            server.start();
            System.out.println("Configuration API running on http://localhost:" + port + "/api/v1");
            System.out.println("Press Ctrl+C to stop");
            server.join();
        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
