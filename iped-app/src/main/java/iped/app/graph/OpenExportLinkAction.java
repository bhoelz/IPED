package iped.app.graph;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class OpenExportLinkAction extends AbstractAction {

    private static final long serialVersionUID = -2484826492967842455L;

    private AppGraphAnalytics app;

    public OpenExportLinkAction(AppGraphAnalytics app) {
        super();
        this.app = app;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ExportLinksDialog dialog = new ExportLinksDialog(app);
        dialog.setVisible(true);
    }

}
