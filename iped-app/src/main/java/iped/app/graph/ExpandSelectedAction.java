package iped.app.graph;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ExpandSelectedAction extends AbstractAction {

    private static final long serialVersionUID = 5212774496202831315L;

    private AppGraphAnalytics app;

    public ExpandSelectedAction(AppGraphAnalytics app) {
        super();
        this.app = app;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        app.expandSelected();
    }

}
