package iped.app.graph;

import org.kharon.layout.HistoryEnabledLayout;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ApplyGraphLayoutAction extends AbstractAction {

    private static final long serialVersionUID = -14285627889421635L;

    private AppGraphAnalytics app;
    private HistoryEnabledLayout layout;

    public ApplyGraphLayoutAction(AppGraphAnalytics app, HistoryEnabledLayout layout) {
        super();
        this.app = app;
        this.layout = layout;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        app.applyLayout(layout);
    }

}
