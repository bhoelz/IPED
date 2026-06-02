package iped.app.graph;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class OpenSearchLinksAction extends AbstractAction {

    private static final long serialVersionUID = -902980467370504606L;

    private AppGraphAnalytics app;

    public OpenSearchLinksAction(AppGraphAnalytics app) {
        super();
        this.app = app;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SearchLinksDialog dialog = new SearchLinksDialog(app);
        dialog.setVisible(true);
    }

}
