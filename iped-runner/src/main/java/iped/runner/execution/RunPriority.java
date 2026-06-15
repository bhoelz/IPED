package iped.runner.execution;

public enum RunPriority {
    LOW(10),
    NORMAL(50),
    HIGH(90);

    /** Higher value = runs first. Used by {@code PriorityBlockingQueue} comparator. */
    public final int weight;

    RunPriority(int weight) {
        this.weight = weight;
    }
}
