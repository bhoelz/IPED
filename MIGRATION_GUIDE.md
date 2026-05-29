# Multi-Case Processing Migration Guide

## For Task/Parser Developers

### No Changes Required

If your task or parser only uses standard IPED APIs (Manager, Statistics, Configuration), **no code changes are needed**. Everything continues working automatically in both single-case and multi-case modes.

```java
// This code works unchanged in multi-case mode
public class MyTask extends AbstractTask {
    @Override
    public void processSourceFiles(List<File> sources) {
        Manager manager = Manager.getInstance();  // Gets correct instance
        Statistics stats = Statistics.get();        // Gets correct instance
        Configuration config = Configuration.getInstance();
        
        // Process as before
    }
}
```

### When to Use Case-Aware APIs (Optional)

If you need to access case-specific information directly, you can opt-in to case awareness:

#### Getting Current Case ID
```java
// Option 1: Via CaseContext (recommended)
CaseContext context = CaseContextThreadLocal.get();
if (context != null) {
    UUID caseId = context.getId();
}

// Option 2: Via CaseData (if you have reference)
UUID caseId = caseData.getSourcePaths().hashCode();  // Not ideal
```

#### Per-Case Configuration Overrides
```java
// Old (single-case)
Configuration config = Configuration.getInstance();
String outputDir = config.getOutputDirectory();

// New (case-aware)
CaseContext context = CaseContextThreadLocal.get();
if (context != null) {
    ConfigurationView configView = context.getConfigurationView();
    String outputDir = configView.getOutputDirectory();  // Case-specific
}
```

#### Case-Specific Event Listeners
```java
// Register listener for current case only
CaseContext context = CaseContextThreadLocal.get();
if (context != null) {
    UUID caseId = context.getId();
    UIPropertyListenerProvider.addPropertyChangeListener(
        caseId,
        new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // Fires only for this case
            }
        }
    );
}
```

### Logging Best Practices

Include case ID in logs for easier debugging in multi-case scenarios:

```java
// Old (ambiguous in multi-case)
logger.info("Started processing");

// New (clear which case)
CaseContext context = CaseContextThreadLocal.get();
String caseLabel = (context != null) ? context.getId().toString() : "none";
logger.info("[Case:{}] Started processing", caseLabel);
```

Use MDC (Mapped Diagnostic Context) if your logging framework supports it:

```java
// Set on case start
String caseId = context.getId().toString();
org.slf4j.MDC.put("caseId", caseId);

// Log normally; caseId automatically included
logger.info("Processing started");

// Clean up
org.slf4j.MDC.remove("caseId");
```

## For Application Integrators

### Initializing Multi-Case Processing

#### Simple Setup (Use Defaults)
```java
// System will use defaults:
// - maxConcurrentCases = Runtime.availableProcessors()
// - maxMemoryPerCase = JVM maxMemory / concurrentCases
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();
```

#### Custom Configuration
```java
ProcessingOrchestratorConfig config = new ProcessingOrchestratorConfig();
config.setMaxConcurrentCases(4);
config.setMaxMemoryPerCase(2_000_000_000L);  // 2GB per case
config.setSharedThreadPoolSize(8);

// Initialize before first getInstance() call
ProcessingOrchestrator.initialize(
    config.getMaxConcurrentCases(),
    config.getMaxMemoryPerCase()
);
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();
```

### Submitting Cases

```java
// 1. Create case context
CaseData caseData = new CaseData();
caseData.setSourcePaths(sources);

ConfigurationView configView = new ConfigurationView();
// Optional: apply case-specific overrides

Manager manager = new Manager();

UUID caseId = UUID.randomUUID();
CaseContext context = new CaseContext.Builder(caseId)
    .withCaseData(caseData)
    .withConfigurationView(configView)
    .withManager(manager)
    .build();

// 2. Enqueue for processing
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();
UUID returnedId = orchestrator.enqueueCaseForProcessing(context);

// 3. Optional: wait for completion with timeout
boolean completed = orchestrator.waitForCompletion(timeout_ms);
```

### Monitoring Cases

```java
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();

// List active cases
List<UUID> activeCases = orchestrator.getActiveCaseIds();

// Check specific case
CaseContext context = orchestrator.getCaseContext(caseId);
if (context != null) {
    System.out.println("Case: " + caseId);
    System.out.println("State: " + context.getState());
    System.out.println("Items: " + context.getStatistics().getProcessedItems());
}

// Get resource stats
ResourceManager resourceMgr = orchestrator.getResourceManager();
System.out.println("Active cases: " + orchestrator.getActiveCaseCount());
System.out.println("Memory used: " + resourceMgr.getTotalMemoryUsage());
System.out.println("Memory limit: " + resourceMgr.getMaxMemoryPerCase());
```

### HTTP Monitoring (Web API)

See Phase 9 implementation for REST endpoints:

```bash
# List active cases
curl http://localhost:8080/cases

# Get case status
curl http://localhost:8080/cases/{caseId}

# Pause case
curl -X POST http://localhost:8080/cases/{caseId}/pause

# Resume case
curl -X POST http://localhost:8080/cases/{caseId}/resume

# Global statistics
curl http://localhost:8080/stats/global

# Case statistics
curl http://localhost:8080/stats/case/{caseId}
```

## Common Patterns

### Pattern 1: Batch Processing with Backpressure
```java
Queue<CaseDefinition> casesToProcess = loadCases();

while (!casesToProcess.isEmpty()) {
    CaseDefinition caseDef = casesToProcess.poll();
    CaseContext context = buildContext(caseDef);
    
    try {
        UUID caseId = orchestrator.enqueueCaseForProcessing(context);
        logger.info("Case {} enqueued", caseId);
    } catch (IllegalStateException e) {
        // Resources exhausted, requeue for later
        casesToProcess.offer(caseDef);
        Thread.sleep(5000);
    }
}

// Wait for all cases to complete
orchestrator.waitForCompletion(Long.MAX_VALUE);
```

### Pattern 2: Priority-Based Processing
```java
// Sort cases by priority before submission
List<CaseDefinition> cases = loadCases();
cases.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

for (CaseDefinition caseDef : cases) {
    CaseContext context = buildContext(caseDef);
    orchestrator.enqueueCaseForProcessing(context);
}
```

### Pattern 3: Monitoring with Dynamic Throttling
```java
ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
monitor.scheduleAtFixedRate(() -> {
    ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();
    ResourceManager resourceMgr = orchestrator.getResourceManager();
    
    long memoryUsed = resourceMgr.getTotalMemoryUsage();
    long memoryLimit = resourceMgr.getMaxMemoryPerCase() * 
                       orchestrator.getActiveCaseCount();
    
    if (memoryUsed > memoryLimit * 0.9) {
        logger.warn("Memory usage at 90%, pausing new submissions");
        stopNewSubmissions();
    } else if (memoryUsed < memoryLimit * 0.7) {
        logger.info("Memory usage normalizing, resuming submissions");
        resumeNewSubmissions();
    }
}, 1, 1, TimeUnit.SECONDS);
```

### Pattern 4: Error Recovery
```java
for (CaseDefinition caseDef : cases) {
    try {
        CaseContext context = buildContext(caseDef);
        UUID caseId = orchestrator.enqueueCaseForProcessing(context);
        
        // Wait for this specific case
        while (orchestrator.getCaseContext(caseId) != null) {
            Thread.sleep(1000);
        }
        
        logger.info("Case {} completed successfully", caseId);
    } catch (Exception e) {
        logger.error("Failed to process case {}: {}", caseDef.getId(), e);
        recordFailure(caseDef);
    }
}
```

## Troubleshooting

### Issue: "Only 1 case processes at a time"
**Cause:** ProcessingOrchestrator not started
```java
// Missing this call
ProcessingOrchestrator orchestrator = ProcessingOrchestrator.getInstance();
orchestrator.startProcessing();  // ← Need this
```

### Issue: "Cases are paused and won't resume"
**Cause:** Memory quota exceeded and not dropping
```java
// Check memory usage
ResourceManager resourceMgr = orchestrator.getResourceManager();
for (UUID caseId : orchestrator.getActiveCaseIds()) {
    long memory = resourceMgr.getMemoryUsage(caseId);
    long limit = resourceMgr.getMaxMemoryPerCase();
    System.out.println(caseId + ": " + memory + "/" + limit);
}

// Solution: Increase quota
ProcessingOrchestrator.initialize(2, 4_000_000_000L);  // 4GB per case
```

### Issue: "Getting wrong Manager/Statistics instance"
**Cause:** CaseContextThreadLocal not set (single-case mode)
```java
// This is expected behavior
CaseContext context = CaseContextThreadLocal.get();
if (context == null) {
    logger.debug("Not in case-specific context, using defaults");
}
```

### Issue: "Context leaked to other threads"
**Cause:** Worker didn't clear ThreadLocal (code bug)
```java
// Correct pattern in Worker
try {
    CaseContextThreadLocal.set(context);
    // ... process ...
} finally {
    CaseContextThreadLocal.clear();  // ← CRITICAL
}
```

## Performance Tuning

### For CPU-Bound Cases
```java
config.setMaxConcurrentCases(Runtime.availableProcessors());
// Full parallelism, no oversubscription
```

### For I/O-Bound Cases
```java
config.setMaxConcurrentCases(Runtime.availableProcessors() * 2);
// Over-subscribe since threads wait on I/O
```

### For Memory-Constrained Environments
```java
config.setMaxMemoryPerCase(500_000_000L);  // 500MB per case
config.setMaxConcurrentCases(4);
// Total: 2GB multi-case limit on 2GB JVM
```

## Testing Multi-Case Code

### Unit Test Template
```java
@Test
void testMyTaskInMultiCase() throws Exception {
    UUID case1Id = UUID.randomUUID();
    UUID case2Id = UUID.randomUUID();
    
    CaseContext context1 = new CaseContext.Builder(case1Id)
        .withCaseData(new CaseData())
        .withConfigurationView(new ConfigurationView())
        .build();
    
    CaseContext context2 = new CaseContext.Builder(case2Id)
        .withCaseData(new CaseData())
        .withConfigurationView(new ConfigurationView())
        .build();
    
    // Test case 1
    CaseContextThreadLocal.set(context1);
    MyTask task1 = new MyTask();
    // assertions...
    CaseContextThreadLocal.clear();
    
    // Test case 2
    CaseContextThreadLocal.set(context2);
    MyTask task2 = new MyTask();
    // assertions...
    CaseContextThreadLocal.clear();
}
```

## Version Compatibility

- **Single-case code:** 100% compatible, no changes needed
- **Existing tasks/parsers:** Work unchanged in multi-case mode
- **Existing tests:** Pass without modification (run in single-case context)

This architecture maintains perfect backwards compatibility while enabling new multi-case capabilities.
