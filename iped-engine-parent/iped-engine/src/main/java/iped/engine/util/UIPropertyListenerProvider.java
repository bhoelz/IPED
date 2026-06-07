package iped.engine.util;

import iped.engine.core.CaseContextThreadLocal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class UIPropertyListenerProvider {

    private static UIPropertyListenerProvider instance = new UIPropertyListenerProvider();

    private ArrayList<PropertyChangeListener> listeners = new ArrayList<>();
    private ArrayList<PropertyChangeListener> uiListeners = new ArrayList<>();

    private final ConcurrentHashMap<UUID, Set<PropertyChangeListener>> caseListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<PropertyChangeListener>> caseUiListeners = new ConcurrentHashMap<>();

    private Thread executorthread;

    private volatile boolean canceled = false;

    /**
     * Dispatcher used to invoke UI listeners. Defaults to inline (synchronous)
     * execution, suitable for headless / test mode. In GUI mode, iped-app sets
     * this to {@code SwingUtilities::invokeLater} so events are marshalled to the
     * EDT automatically.
     */
    private Consumer<Runnable> uiDispatcher = Runnable::run;

    private UIPropertyListenerProvider() {
    }

    public static UIPropertyListenerProvider getInstance() {
        return instance;
    }

    public void setExecutorThread(Thread executorthread) {
        this.executorthread = executorthread;
    }

    /**
     * Sets the dispatcher used to invoke UI listeners.
     * Call {@code setUiDispatcher(SwingUtilities::invokeLater)} from iped-app
     * before processing starts when running in GUI mode.
     */
    public void setUiDispatcher(Consumer<Runnable> dispatcher) {
        this.uiDispatcher = dispatcher;
    }

    public void addPropertyChangeListener(PropertyChangeListener l, boolean isUIListener) {
        if (isUIListener) {
            uiListeners.add(l);
        } else {
            listeners.add(l);
        }
    }

    public void addPropertyChangeListener(UUID caseId, PropertyChangeListener l, boolean isUIListener) {
        if (isUIListener) {
            caseUiListeners.computeIfAbsent(caseId, k -> ConcurrentHashMap.newKeySet()).add(l);
        } else {
            caseListeners.computeIfAbsent(caseId, k -> ConcurrentHashMap.newKeySet()).add(l);
        }
    }

    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);

        var ctx = CaseContextThreadLocal.get();
        if (ctx != null) {
            Set<PropertyChangeListener> caseListenersSet = caseListeners.get(ctx.getId());
            if (caseListenersSet != null) {
                for (PropertyChangeListener l : caseListenersSet) {
                    l.propertyChange(event);
                }
            }

            Set<PropertyChangeListener> caseUiListenersSet = caseUiListeners.get(ctx.getId());
            if (caseUiListenersSet != null) {
                for (PropertyChangeListener l : caseUiListenersSet) {
                    uiDispatcher.accept(() -> l.propertyChange(event));
                }
            }
        }

        for (PropertyChangeListener l : listeners) {
            l.propertyChange(event);
        }
        for (PropertyChangeListener l : uiListeners) {
            uiDispatcher.accept(() -> l.propertyChange(event));
        }
    }

    public boolean isCancelled() {
        return canceled;
    }

    public void cancel(boolean interrupt) {
        canceled = true;
        if (interrupt && executorthread != null) {
            executorthread.interrupt();
        }
    }

}
