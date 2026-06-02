package iped.app.graph;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RedoAction extends AbstractAction {

    private static final long serialVersionUID = 5853095468097097009L;

    private AppGraphAnalytics app;

    public RedoAction(AppGraphAnalytics app) {
        super();
        this.app = app;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        this.app.redo();
    }

}
