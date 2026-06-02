package iped.engine.util;

import iped.engine.config.ConfigurationView;
import iped.engine.core.CaseContext;
import iped.engine.core.CaseContextThreadLocal;
import iped.engine.data.CaseData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests to verify UIPropertyListenerProvider routes events per case.
 *
 * These tests verify that property change events are routed to case-specific
 * listeners when in a multi-case context, while maintaining backward compatibility
 * with global listeners.
 */
public class UIPropertyListenerProviderTest {

    @AfterEach
    void tearDown() {
        CaseContextThreadLocal.clear();
    }

    @Test
    void testCaseSpecificListenerRoutingBetweenThreads() throws InterruptedException {
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

        List<PropertyChangeEvent> case1Events = new ArrayList<>();
        List<PropertyChangeEvent> case2Events = new ArrayList<>();

        PropertyChangeListener listener1 = case1Events::add;
        PropertyChangeListener listener2 = case2Events::add;

        UIPropertyListenerProvider provider = UIPropertyListenerProvider.getInstance();

        Thread t1 = new Thread(() -> {
            CaseContextThreadLocal.set(context1);
            provider.addPropertyChangeListener(case1Id, listener1, false);
            provider.firePropertyChange("case1", null, "value1");
            CaseContextThreadLocal.clear();
        });

        Thread t2 = new Thread(() -> {
            CaseContextThreadLocal.set(context2);
            provider.addPropertyChangeListener(case2Id, listener2, false);
            provider.firePropertyChange("case2", null, "value2");
            CaseContextThreadLocal.clear();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(1, case1Events.size(), "Case 1 should receive 1 event");
        assertEquals("case1", case1Events.get(0).getPropertyName());
        assertEquals("value1", case1Events.get(0).getNewValue());

        assertEquals(1, case2Events.size(), "Case 2 should receive 1 event");
        assertEquals("case2", case2Events.get(0).getPropertyName());
        assertEquals("value2", case2Events.get(0).getNewValue());
    }

    @Test
    void testCaseSpecificListenersIsolated() throws InterruptedException {
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

        List<PropertyChangeEvent> case1Events = new ArrayList<>();
        List<PropertyChangeEvent> case2Events = new ArrayList<>();

        PropertyChangeListener listener1 = case1Events::add;
        PropertyChangeListener listener2 = case2Events::add;

        UIPropertyListenerProvider provider = UIPropertyListenerProvider.getInstance();

        provider.addPropertyChangeListener(case1Id, listener1, false);
        provider.addPropertyChangeListener(case2Id, listener2, false);

        Thread t1 = new Thread(() -> {
            CaseContextThreadLocal.set(context1);
            provider.firePropertyChange("event", null, "data");
            CaseContextThreadLocal.clear();
        });

        Thread t2 = new Thread(() -> {
            CaseContextThreadLocal.set(context2);
            provider.firePropertyChange("event", null, "data");
            CaseContextThreadLocal.clear();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        assertEquals(1, case1Events.size(), "Case 1 should receive only its own event");
        assertEquals(1, case2Events.size(), "Case 2 should receive only its own event");
    }

    @Test
    void testMultipleCaseListenersPerCase() throws InterruptedException {
        UUID caseId = UUID.randomUUID();

        CaseContext context = new CaseContext.Builder(caseId)
                .withCaseData(new CaseData())
                .withConfigurationView(new ConfigurationView())
                .build();

        List<PropertyChangeEvent> events1 = new ArrayList<>();
        List<PropertyChangeEvent> events2 = new ArrayList<>();

        PropertyChangeListener listener1 = events1::add;
        PropertyChangeListener listener2 = events2::add;

        UIPropertyListenerProvider provider = UIPropertyListenerProvider.getInstance();
        provider.addPropertyChangeListener(caseId, listener1, false);
        provider.addPropertyChangeListener(caseId, listener2, false);

        CaseContextThreadLocal.set(context);
        provider.firePropertyChange("test", null, "value");
        CaseContextThreadLocal.clear();

        assertEquals(1, events1.size(), "Listener 1 should receive event");
        assertEquals(1, events2.size(), "Listener 2 should receive event");
        assertEquals("test", events1.get(0).getPropertyName());
        assertEquals("test", events2.get(0).getPropertyName());
    }
}
