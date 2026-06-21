# Multi-Case Processing - Quick Start Guide

## TL;DR

**For Existing Tasks/Parsers:** No changes needed. Your code works unchanged in multi-case mode.

**For New Multi-Case Code:** 10 lines to submit a case, rest is automatic.

## Simplest Example: Submit One Case

```java
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();

CaseContext context = new CaseContext.Builder(UUID.randomUUID())
    .withCaseData(new CaseData())
    .withConfigurationView(new ConfigurationView())
    .build();

orchestrator.enqueueCaseForProcessing(context);
```

That's it. The case will:
- Enqueue automatically
- Start when resources available
- Process with isolated Manager/Statistics
- Clean up when done

## Monitoring Cases

```java
// List active cases
for (UUID caseId : orchestrator.getActiveCaseIds()) {
    CaseContext ctx = orchestrator.getCaseContext(caseId);
    System.out.println(caseId + ": " + ctx.getState());
}

// Pause a case
orchestrator.pauseCase(caseId);

// Resume a case
orchestrator.resumeCase(caseId);

// Wait for all to complete
orchestrator.waitForCompletion(60_000);  // timeout in ms
```

## HTTP Monitoring (REST API)

```bash
# List active cases
curl http://localhost:8080/cases

# Get case status
curl http://localhost:8080/cases/{caseId}

# Pause case
curl -X POST http://localhost:8080/cases/{caseId}/pause

# Resume case
curl -X POST http://localhost:8080/cases/{caseId}/resume

# Global stats
curl http://localhost:8080/stats/global

# Case stats
curl http://localhost:8080/stats/case/{caseId}
```

## Configuration

```java
// Use defaults (based on system cores)
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();

// OR use custom config
ProcessingOrchestrator.initialize(4, 2_000_000_000L);  // 4 cases, 2GB each
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();
```

## Common Patterns

### Pattern 1: Batch Processing
```java
List<CaseDefinition> cases = loadCasesFromFile("cases.txt");
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();

for (CaseDefinition caseDef : cases) {
    CaseContext context = buildContext(caseDef);
    try {
        orchestrator.enqueueCaseForProcessing(context);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

orchestrator.waitForCompletion(Long.MAX_VALUE);
System.out.println("All cases processed!");
```

### Pattern 2: Progress Monitoring
```java
ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
monitor.scheduleAtFixedRate(() -> {
    ProcessingOrchestrator orch = ProcessingOrchestrator.getInstance();
    System.out.println("Active cases: " + orch.getActiveCaseCount());
    System.out.println("Memory: " + orch.getResourceManager().getTotalMemoryUsage());
}, 0, 5, TimeUnit.SECONDS);
```

### Pattern 3: Error Handling
```java
for (CaseDefinition caseDef : cases) {
    try {
        CaseContext context = buildContext(caseDef);
        orchestrator.enqueueCaseForProcessing(context);
    } catch (IllegalStateException e) {
        // Resources exhausted, queue is full
        logger.warn("Cannot enqueue case {}: {}", caseDef.getId(), e.getMessage());
        recordFailure(caseDef);
    } catch (InterruptedException e) {
        logger.error("Interrupted while enqueueing case {}", caseDef.getId());
        Thread.currentThread().interrupt();
        break;
    }
}
```

## Troubleshooting

### "Only 1 case runs at a time"
Check that you're calling `enqueueCaseForProcessing()` (it auto-starts), not manually calling `startProcessing()`.

### "Case stays PAUSED"
Memory limit exceeded. Check `/stats/case/{id}` to see memory usage. Increase quota if needed.

### "Got IllegalStateException when enqueueing"
Max concurrent cases reached. Wait for one to complete or increase `maxConcurrentCases`.

### "Different thread has wrong Manager"
Each thread gets its own case context via ThreadLocal. Set context before processing:
```java
CaseContextThreadLocal.set(context);
try {
    // process...
} finally {
    CaseContextThreadLocal.clear();
}
```

## Your Existing Tasks/Parsers

**No changes needed.** They use Manager.getInstance() and Statistics.get() which now automatically return the correct case-specific instance via ThreadLocal delegation.

```java
// This works unchanged
public class MyTask extends AbstractTask {
    public void run() {
        Manager manager = Manager.getInstance();  // ← Gets correct case manager
        Statistics stats = Statistics.get();      // ← Gets correct case stats
        // ... rest of code unchanged ...
    }
}
```

## Going Deeper

- See [ARCHITECTURE.md](ARCHITECTURE.md) for system design and implementation status
- See [MULTI-CASE-MIGRATION-GUIDE.md](MULTI-CASE-MIGRATION-GUIDE.md) for detailed API docs

## Key Classes to Know

- `ProcessingOrchestrator` - Main coordinator (singleton)
- `CaseContext` - Immutable case state container
- `CaseContextThreadLocal` - ThreadLocal accessor
- `ResourceManager` - Memory/concurrency quota enforcement
- `Cases.java` - HTTP endpoints for case management
- `Stats.java` - HTTP endpoints for monitoring

## API Reference

### ProcessingOrchestrator

```java
// Lifecycle
UUID id = orchestrator.enqueueCaseForProcessing(context);
orchestrator.pauseCase(caseId);
orchestrator.resumeCase(caseId);
orchestrator.completeCaseProcessing(caseId);

// Monitoring
int count = orchestrator.getActiveCaseCount();
List<UUID> ids = orchestrator.getActiveCaseIds();
CaseContext ctx = orchestrator.getCaseContext(caseId);
boolean done = orchestrator.waitForCompletion(timeout);

// Configuration
ResourceManager mgr = orchestrator.getResourceManager();
```

### CaseContext

```java
// Construction (use Builder)
CaseContext context = new CaseContext.Builder(UUID.randomUUID())
    .withCaseData(caseData)
    .withConfigurationView(configView)
    .withManager(manager)
    .build();

// Access
UUID id = context.getId();
Manager mgr = context.getManager();
Statistics stats = context.getStatistics();
CaseState state = context.getState();
```

### CaseContextThreadLocal

```java
// Set current case context
CaseContextThreadLocal.set(context);

// Get current case context
CaseContext ctx = CaseContextThreadLocal.get();

// Clear (important: do in finally block)
CaseContextThreadLocal.clear();
```

## Performance Tips

1. **Set reasonable quotas**: Too low = cases keep pausing. Too high = risk OOM.
2. **Monitor memory usage**: Use `/stats/global` endpoint frequently.
3. **Batch submission**: Submit all cases first, then wait, not one at a time.
4. **Use HTTP monitoring**: Lighter weight than polling via Java API.

## Next Steps

1. Read [MULTI-CASE-MIGRATION-GUIDE.md](MULTI-CASE-MIGRATION-GUIDE.md) if implementing custom integration
2. Check your existing code - it likely works unchanged
3. Use [ARCHITECTURE.md](ARCHITECTURE.md) as reference for design questions
4. Run the Phase 10 tests to validate in your environment

## Support

- Tests: `iped-engine-parent/iped-engine/src/test/java/iped/engine/core/`
- Documentation: this module's `README.md` and the files above
- HTTP API: `iped-webapi/src/main/java/iped/engine/webapi/Cases.java` and `Stats.java`
