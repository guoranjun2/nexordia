package org.freeplane.view.swing.map.outline;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

class FocusSelectedButtonClickAdapter extends MouseAdapter {
    private final ScrollableTreePanel panel;

    FocusSelectedButtonClickAdapter(ScrollableTreePanel panel) {
        this.panel = panel;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        panel.focusSelectionButtonLater(true);
    }
}

