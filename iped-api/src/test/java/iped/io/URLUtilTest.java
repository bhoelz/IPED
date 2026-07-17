package iped.io;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import org.junit.jupiter.api.Test;

class URLUtilTest {

  @Test
  void getURL_whenCalledWithClass_thenReturnsValidUrl() {
    URL url = URLUtil.getURL(URLUtilTest.class);

    assertNotNull(url);
  }

  @Test
  void getURL_whenWindowsUncLikePath_thenAppliesPathFix() throws Exception {
    URL sourceUrl = new URL("file:////server/share/test.jar");
    CodeSource codeSource = new CodeSource(sourceUrl, (Certificate[]) null);
    PermissionCollection permissions = new Permissions();
    ProtectionDomain domain = new ProtectionDomain(codeSource, permissions);

    URL result = URLUtil.getURL(domain);

    assertNotNull(result);
    assertEquals("file", result.getProtocol());
    assertTrue(result.toExternalForm().startsWith("file:///"));
  }
}
