package iped.engine.webapi;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import iped.engine.Version;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

/** Main class. */
public class Main {
  /**
   * Starts Jetty HTTP server exposing JAX-RS resources defined in this application.
   *
   * @return Jetty HTTP server.
   * @throws IOException
   * @throws ParseException
   */
  public static Server startServer(String host, int port, String urlToAskSources)
      throws IOException {
    // create a resource config that scans for JAX-RS resources and providers
    // in gpinf.api package
    String resources = Main.class.getPackageName();
    SwaggerConfiguration swaggerConfiguration =
        new SwaggerConfiguration()
            .resourcePackages(Collections.singleton(resources))
            .openAPI(
                new OpenAPI().info(new Info().title("IPED Web API").version(Version.APP_VERSION)));
    final ResourceConfig rc =
        new ResourceConfig()
            .packages(resources)
            .register(new OpenApiResource().openApiConfiguration(swaggerConfiguration));

    try {
      // urlToAskSources may be null — the engine handles that as zero open cases.
      Sources.init(urlToAskSources);
    } catch (IllegalArgumentException e) {
      // A bad source path is a user error; give a clear message and abort startup.
      throw new IOException("Invalid source configuration: " + e.getMessage(), e);
    } catch (Exception e) {
      throw new IOException("Failed to initialize sources", e);
    }

    // create and start a new instance of jetty http server
    // exposing the Jersey application at BASE_URI
    return JettyHttpContainerFactory.createServer(URI.create("http://" + host + ":" + port), rc);
  }

  /**
   * Main method.
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws Exception {
    String host = "0.0.0.0";
    int port = 8080;
    String urlToAskSources = null;

    for (String arg : args) {
      if (arg.startsWith("--host=")) {
        host = arg.substring("--host=".length());

      } else if (arg.startsWith("--port=")) {
        port = Integer.parseInt(arg.substring("--port=".length()));

      } else if (arg.startsWith("--sources=")) {
        urlToAskSources = arg.substring("--sources=".length());

      } else {
        printHelp();
        System.exit(-1);
      }
    }
    // --sources is optional; omitting it starts the server with no open cases.
    // Cases can be added at runtime via POST /sources.
    startServer(host, port, urlToAskSources);
    System.out.println("IPED Web API started at http://" + host + ":" + port + "/");
    System.out.println("OpenAPI spec: http://" + host + ":" + port + "/openapi.json");
    if (urlToAskSources == null) {
      System.out.println("No --sources provided. POST a source to /sources to open a case.");
    }
  }

  public static void printHelp() {
    System.out.println("--sources=(URL|Path)\tfile or url with json: [{id, path}...]");
    System.out.println("--host=\t\tdefault:0.0.0.0");
    System.out.println("--port=\t\tdefault:8080");
  }
}
