package iped.distributed.coordinator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

/**
 * Snapshot of a Task Agent's identity and current capacity.
 * Published on registration and refreshed on each heartbeat.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentRegistration {

    private String  agentId;
    private String  taskType;         // e.g. "HashTask"
    private int     stageNumber;
    private String  hostname;
    private int     maxParallelItems;
    private int     currentLoad;      // items currently being processed
    private int     freeSlots;
    private Instant lastHeartbeat;

    public static AgentRegistration of(String agentId, String taskType, int stageNumber,
                                        int maxParallelItems, String hostname) {
        AgentRegistration r = new AgentRegistration();
        r.agentId          = agentId;
        r.taskType         = taskType;
        r.stageNumber      = stageNumber;
        r.maxParallelItems = maxParallelItems;
        r.freeSlots        = maxParallelItems;
        r.currentLoad      = 0;
        r.hostname         = hostname;
        r.lastHeartbeat    = Instant.now();
        return r;
    }

    // Getters / Setters
    public String  getAgentId()           { return agentId; }
    public void    setAgentId(String v)   { agentId = v; }

    public String  getTaskType()          { return taskType; }
    public void    setTaskType(String v)  { taskType = v; }

    public int     getStageNumber()       { return stageNumber; }
    public void    setStageNumber(int v)  { stageNumber = v; }

    public String  getHostname()          { return hostname; }
    public void    setHostname(String v)  { hostname = v; }

    public int     getMaxParallelItems()  { return maxParallelItems; }
    public void    setMaxParallelItems(int v) { maxParallelItems = v; }

    public int     getCurrentLoad()       { return currentLoad; }
    public void    setCurrentLoad(int v)  { currentLoad = v; }

    public int     getFreeSlots()         { return freeSlots; }
    public void    setFreeSlots(int v)    { freeSlots = v; }

    public Instant getLastHeartbeat()     { return lastHeartbeat; }
    public void    setLastHeartbeat(Instant v) { lastHeartbeat = v; }
}
