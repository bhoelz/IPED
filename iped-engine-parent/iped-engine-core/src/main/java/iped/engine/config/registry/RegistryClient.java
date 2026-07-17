package iped.engine.config.registry;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;

public class RegistryClient {

  private final HttpClient http;
  private final PluginVerifier verifier;

  public RegistryClient(PluginVerifier verifier) {
    this.http =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    this.verifier = verifier;
  }

  public RegistryIndex fetchAndValidateIndex(URI indexUri, PublicKeyStore keyStore)
      throws IOException, InterruptedException {
    HttpRequest req =
        HttpRequest.newBuilder(indexUri).GET().timeout(Duration.ofSeconds(20)).build();
    HttpResponse<String> resp =
        http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    if (resp.statusCode() / 100 != 2) {
      throw new IOException("Index fetch failed: HTTP " + resp.statusCode());
    }

    String json = resp.body();
    RegistryIndex index = RegistryJson.parseIndex(json);
    RegistrySchemaValidator.validate(index);
    verifier.verifyIndexSignature(json, index.signature(), keyStore);
    return index;
  }

  public Path downloadArtifact(URI artifactUri, Path cacheDir)
      throws IOException, InterruptedException {
    Files.createDirectories(cacheDir);
    Path target = cacheDir.resolve(Path.of(artifactUri.getPath()).getFileName().toString());
    HttpRequest req =
        HttpRequest.newBuilder(artifactUri).GET().timeout(Duration.ofMinutes(2)).build();
    HttpResponse<Path> resp = http.send(req, HttpResponse.BodyHandlers.ofFile(target));
    if (resp.statusCode() / 100 != 2) {
      throw new IOException("Artifact download failed: HTTP " + resp.statusCode());
    }
    return target;
  }

  public void validateDownloadedArtifact(
      Path jar, RegistryIndex.PluginVersion version, PublicKeyStore keyStore) throws Exception {
    verifier.verifyFileSha256(jar, version.artifact().sha256());
    if (version.signature() != null) {
      Path sig = downloadArtifact(URI.create(version.signature().sigUrl()), jar.getParent());
      verifier.verifyArtifactSignature(jar, sig, version.signature(), keyStore);
    }
    verifier.verifyServiceLoaderContract(
        jar, version.provider().providerClass(), version.provider().taskId());
  }

  public static String sha256(Path file) throws Exception {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(Files.readAllBytes(file));
    return HexFormat.of().formatHex(md.digest());
  }
}
