package iped.app.graph;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class RemoveSelectedAction extends AbstractAction {

    private static final long serialVersionUID = -150299378093154838L;

    private AppGraphAnalytics app;

    public RemoveSelectedAction(AppGraphAnalytics app) {
        super();
        this.app = app;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        app.removeSelected();
    }

}
