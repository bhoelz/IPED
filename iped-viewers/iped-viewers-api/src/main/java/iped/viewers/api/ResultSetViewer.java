package iped.viewers.api;

import bibliothek.gui.dock.common.DefaultSingleCDockable;

import javax.swing.*;

/*
 * Defines a viewer for a set of results controlled by a MultiSearchResultProvider
 */

public interface ResultSetViewer {

    public void init(JTable resultsTable, IMultiSearchResultProvider resultsProvider, GUIProvider guiProvider);

    public void setDockableContainer(DefaultSingleCDockable dockable);

    public String getTitle();

    public String getID();

    public JPanel getPanel();

    public void redraw();

    public void updateSelection();

    public void checkAll(boolean value);

    public GUIProvider getGUIProvider();

    public void notifyCaseDataChanged();

}
