package iped.engine.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

public class CliArgsTest {

  @Test
  void parsesRequiredUrlWithDefaults() {
    CliArgs args = CliArgs.parse(new String[] {"--webapi-url=http://localhost:8080"});
    assertEquals("http://localhost:8080", args.webapiUrl());
    assertEquals("stdio", args.transport());
    assertEquals(3000, args.port());
    assertNull(args.apiKey());
    assertTrue(args.allowedCases().isEmpty());
    assertEquals(120, args.rateLimit());
    assertTrue(args.capabilities().isEmpty());
  }

  @Test
  void parsesAllArguments() {
    CliArgs args =
        CliArgs.parse(
            new String[] {
              "--webapi-url=http://host:1234",
              "--transport=http",
              "--port=9000",
              "--api-key=secret",
              "--allowed-cases=case1,case2",
              "--rate-limit=60",
              "--capabilities=bookmarks,jobs"
            });
    assertEquals("http://host:1234", args.webapiUrl());
    assertEquals("http", args.transport());
    assertEquals(9000, args.port());
    assertEquals("secret", args.apiKey());
    assertEquals(Set.of("case1", "case2"), args.allowedCases());
    assertEquals(60, args.rateLimit());
    assertEquals(
        Set.of(GrantedCapabilities.BOOKMARKS, GrantedCapabilities.JOBS), args.capabilities());
  }

  @Test
  void parsesCapabilitiesSubset() {
    CliArgs args = CliArgs.parse(new String[] {"--webapi-url=http://x", "--capabilities=jobs"});
    assertEquals(Set.of(GrantedCapabilities.JOBS), args.capabilities());
  }

  @Test
  void invalidCapabilityReturnsNull() {
    assertNull(CliArgs.parse(new String[] {"--webapi-url=http://x", "--capabilities=unknown"}));
  }

  @Test
  void allowedCasesTrimmed() {
    CliArgs args =
        CliArgs.parse(new String[] {"--webapi-url=http://x", "--allowed-cases= a , b , c "});
    assertEquals(Set.of("a", "b", "c"), args.allowedCases());
  }

  @Test
  void missingUrlReturnsNull() {
    assertNull(CliArgs.parse(new String[0]));
    assertNull(CliArgs.parse(new String[] {"--transport=http"}));
  }

  @Test
  void unknownArgumentReturnsNull() {
    assertNull(CliArgs.parse(new String[] {"--webapi-url=http://x", "--bogus=1"}));
  }

  @Test
  void invalidPortReturnsNull() {
    assertNull(CliArgs.parse(new String[] {"--webapi-url=http://x", "--port=abc"}));
  }

  @Test
  void invalidRateLimitReturnsNull() {
    assertNull(CliArgs.parse(new String[] {"--webapi-url=http://x", "--rate-limit=0"}));
  }
}
