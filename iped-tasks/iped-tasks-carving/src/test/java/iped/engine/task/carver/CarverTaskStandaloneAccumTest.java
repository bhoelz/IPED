package iped.engine.task.carver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers the standalone (caseData == null) fallback of the carve tasks'
 * accumulators: shared across per-item task instances within a run, and
 * cleared by finish() so successive standalone runs in the same JVM start
 * fresh (see BaseCarveTask.clearStandaloneAccum()).
 */
class CarverTaskStandaloneAccumTest {

    private static Object accumOf(Object task, Class<?> declaring) throws Exception {
        Method m = declaring.getDeclaredMethod("accum");
        m.setAccessible(true);
        return m.invoke(task);
    }

    @Test
    void standaloneAccumulatorIsSharedAcrossInstancesAndClearedByFinish() throws Exception {
        CarverTask t1 = new CarverTask();
        CarverTask t2 = new CarverTask();

        Object a1 = accumOf(t1, CarverTask.class);
        assertSame(a1, accumOf(t2, CarverTask.class),
                "standalone accumulator must be shared across per-item task instances");

        Object base1 = accumOf(t1, BaseCarveTask.class);
        assertSame(base1, accumOf(t2, BaseCarveTask.class),
                "base standalone accumulator must be shared across per-item task instances");

        t1.finish();

        assertNotSame(a1, accumOf(t1, CarverTask.class),
                "finish() must drop the standalone accumulator so the next run starts fresh");
        assertNotSame(base1, accumOf(t1, BaseCarveTask.class),
                "finish() must drop the base standalone accumulator so the next run starts fresh");
    }
}
