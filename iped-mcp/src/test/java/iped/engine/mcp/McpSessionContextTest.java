package iped.engine.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import org.junit.jupiter.api.Test;

class McpSessionContextTest {

  @Test
  void sessionIdIsNonBlank() {
    McpSessionContext ctx = new McpSessionContext(Set.of(), 60);
    assertNotNull(ctx.sessionId);
    assertFalse(ctx.sessionId.isBlank());
  }

  @Test
  void noRestrictionAllowsAnyCaseId() {
    McpSessionContext ctx = new McpSessionContext(Set.of(), 60);
    assertFalse(ctx.hasCaseRestriction());
    assertDoesNotThrow(() -> ctx.check("anything"));
  }

  @Test
  void allowlistPermitsListedCases() {
    McpSessionContext ctx = new McpSessionContext(Set.of("case1", "case2"), 60);
    assertTrue(ctx.hasCaseRestriction());
    assertDoesNotThrow(() -> ctx.check("case1"));
    assertDoesNotThrow(() -> ctx.check("case2"));
  }

  @Test
  void allowlistBlocksUnlistedCase() {
    McpSessionContext ctx = new McpSessionContext(Set.of("case1"), 60);
    var ex =
        assertThrows(McpSessionContext.CaseAccessDeniedException.class, () -> ctx.check("case99"));
    assertTrue(ex.getMessage().contains("case99"));
    assertTrue(ex.getMessage().contains("case1"));
  }

  @Test
  void rateLimiterIsShared() {
    McpSessionContext ctx = new McpSessionContext(Set.of(), 3);
    ctx.rateLimiter.acquire();
    ctx.rateLimiter.acquire();
    ctx.rateLimiter.acquire();
    assertThrows(ToolRateLimiter.RateLimitException.class, ctx.rateLimiter::acquire);
  }
}
