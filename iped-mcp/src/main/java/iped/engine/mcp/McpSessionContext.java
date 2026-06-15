package iped.engine.mcp;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable context for one MCP server process (= one analyst session).
 *
 * <p>Holds the session UUID used for request tracing, the case-ID allowlist
 * (empty = all cases accessible), the write-side capability grants, and the
 * shared rate limiter. All tools receive this context and call
 * {@link #check(String)} before acting on a case-scoped request.
 */
public final class McpSessionContext {

    /** UUID stamped on every outbound WebApiClient request as {@code X-MCP-Session-Id}. */
    public final String sessionId;

    /**
     * Allowed case IDs for this session. Empty means no restriction.
     * Populated from {@code --allowed-cases=id1,id2} CLI arg.
     */
    public final Set<String> allowedCases;

    /**
     * Write-side capability grants for this session.
     * Populated from {@code --capabilities=bookmarks,jobs} CLI arg.
     * Empty means read-only (no mutating tools registered).
     */
    public final Set<GrantedCapabilities> capabilities;

    /** Shared rate limiter enforced before every tool invocation. */
    public final ToolRateLimiter rateLimiter;

    public McpSessionContext(Set<String> allowedCases, int maxCallsPerMinute) {
        this(allowedCases, maxCallsPerMinute, java.util.Collections.emptySet());
    }

    public McpSessionContext(Set<String> allowedCases, int maxCallsPerMinute,
                             Set<GrantedCapabilities> capabilities) {
        this.sessionId    = UUID.randomUUID().toString();
        this.allowedCases = Set.copyOf(allowedCases);
        this.capabilities = Set.copyOf(capabilities);
        this.rateLimiter  = new ToolRateLimiter(maxCallsPerMinute);
    }

    public boolean can(GrantedCapabilities cap) {
        return capabilities.contains(cap);
    }

    /**
     * Asserts that {@code caseId} is accessible in this session.
     *
     * @throws CaseAccessDeniedException when an allowlist is configured and
     *         {@code caseId} is not in it
     */
    public void check(String caseId) {
        if (!allowedCases.isEmpty() && !allowedCases.contains(caseId)) {
            throw new CaseAccessDeniedException(caseId, allowedCases);
        }
    }

    /** Returns true when a case-ID allowlist is active. */
    public boolean hasCaseRestriction() {
        return !allowedCases.isEmpty();
    }

    public static final class CaseAccessDeniedException extends RuntimeException {
        public CaseAccessDeniedException(String caseId, Set<String> allowed) {
            super("Access denied: case \"" + caseId + "\" is not in the allowed-cases list "
                    + "for this session. Allowed: " + allowed);
        }
    }
}
