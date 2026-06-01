package iped.engine.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

import iped.engine.core.CaseContextThreadLocal;

public class UIPropertyListenerProvider {

    private static UIPropertyListenerProvider instance = new UIPropertyListenerProvider();

    private ArrayList<PropertyChangeListener> listeners = new ArrayList<>();
    private ArrayList<PropertyChangeListener> uiListeners = new ArrayList<>();

    private final ConcurrentHashMap<UUID, Set<PropertyChangeListener>> caseListeners = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Set<PropertyChangeListener>> caseUiListeners = new ConcurrentHashMap<>();

    private Thread executorthread;

    private volatile boolean canceled = false;

    private UIPropertyListenerProvider() {
    }

    public static UIPropertyListenerProvider getInstance() {
        return instance;
    }

    public void setExecutorThread(Thread executorthread) {
        this.executorthread = executorthread;
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
                    if (SwingUtilities.isEventDispatchThread()) {
                        l.propertyChange(event);
                    } else {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                l.propertyChange(event);
                            }
                        });
                    }
                }
            }
        }

        for (PropertyChangeListener l : listeners) {
            l.propertyChange(event);
        }
        for (PropertyChangeListener l : uiListeners) {
            if (SwingUtilities.isEventDispatchThread()) {
                l.propertyChange(event);
            } else {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        l.propertyChange(event);
                    }
                });
            }
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
