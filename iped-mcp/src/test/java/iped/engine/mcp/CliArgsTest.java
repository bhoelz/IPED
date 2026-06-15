package iped.engine.mcp;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class CliArgsTest {

    @Test
    void parsesRequiredUrlWithDefaults() {
        CliArgs args = CliArgs.parse(new String[]{"--webapi-url=http://localhost:8080"});
        assertEquals("http://localhost:8080", args.webapiUrl());
        assertEquals("stdio", args.transport());
        assertEquals(3000, args.port());
        assertNull(args.apiKey());
        assertTrue(args.allowedCases().isEmpty());
        assertEquals(120, args.rateLimit());
    }

    @Test
    void parsesAllArguments() {
        CliArgs args = CliArgs.parse(new String[]{
                "--webapi-url=http://host:1234",
                "--transport=http",
                "--port=9000",
                "--api-key=secret",
                "--allowed-cases=case1,case2",
                "--rate-limit=60"
        });
        assertEquals("http://host:1234", args.webapiUrl());
        assertEquals("http", args.transport());
        assertEquals(9000, args.port());
        assertEquals("secret", args.apiKey());
        assertEquals(Set.of("case1", "case2"), args.allowedCases());
        assertEquals(60, args.rateLimit());
    }

    @Test
    void allowedCasesTrimmed() {
        CliArgs args = CliArgs.parse(new String[]{
                "--webapi-url=http://x", "--allowed-cases= a , b , c "});
        assertEquals(Set.of("a", "b", "c"), args.allowedCases());
    }

    @Test
    void missingUrlReturnsNull() {
        assertNull(CliArgs.parse(new String[0]));
        assertNull(CliArgs.parse(new String[]{"--transport=http"}));
    }

    @Test
    void unknownArgumentReturnsNull() {
        assertNull(CliArgs.parse(new String[]{"--webapi-url=http://x", "--bogus=1"}));
    }

    @Test
    void invalidPortReturnsNull() {
        assertNull(CliArgs.parse(new String[]{"--webapi-url=http://x", "--port=abc"}));
    }

    @Test
    void invalidRateLimitReturnsNull() {
        assertNull(CliArgs.parse(new String[]{"--webapi-url=http://x", "--rate-limit=0"}));
    }
}
