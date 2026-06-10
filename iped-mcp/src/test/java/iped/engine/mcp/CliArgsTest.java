package iped.engine.mcp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CliArgsTest {

    @Test
    public void parsesRequiredUrlWithDefaults() {
        CliArgs args = CliArgs.parse(new String[] { "--webapi-url=http://localhost:8080" });
        assertEquals("http://localhost:8080", args.webapiUrl());
        assertEquals("stdio", args.transport());
        assertEquals(3000, args.port());
    }

    @Test
    public void parsesAllArguments() {
        CliArgs args = CliArgs.parse(new String[] {
                "--webapi-url=http://host:1234", "--transport=http", "--port=9000" });
        assertEquals("http://host:1234", args.webapiUrl());
        assertEquals("http", args.transport());
        assertEquals(9000, args.port());
    }

    @Test
    public void missingUrlReturnsNull() {
        assertNull(CliArgs.parse(new String[0]));
        assertNull(CliArgs.parse(new String[] { "--transport=http" }));
    }

    @Test
    public void unknownArgumentReturnsNull() {
        assertNull(CliArgs.parse(new String[] { "--webapi-url=http://x", "--bogus=1" }));
    }

    @Test
    public void invalidPortReturnsNull() {
        assertNull(CliArgs.parse(new String[] { "--webapi-url=http://x", "--port=abc" }));
    }
}
