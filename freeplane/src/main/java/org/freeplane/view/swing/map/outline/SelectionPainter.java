package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.Icon;

final class SelectionPainter {
    private SelectionPainter() {}

    static void paintForBlockPanel(BlockPanel panel, ScrollableTreePanel parentPanel, OutlineSelection selection, Graphics g) {
        if (panel == null || parentPanel == null || selection == null || g == null) return;
        final NodePositioning positioning = parentPanel.getNodePositioning();
        final Icon icon = parentPanel.getSelectionIcon();
        for (Component comp : panel.getComponents()) {
            if (comp instanceof NodeButton) {
                NodeButton btn = (NodeButton) comp;
                TreeNode node = btn.getNode();
                if (node != null && selection.isSelected(node)) {
                    if (! parentPanel.isNodeInBreadcrumbArea(node)) {
                        Point p = positioning.calculateSelectionIconPosition(comp.getBounds());
                        if (p != null) icon.paintIcon(panel, g, p.x, p.y);
                    }
                }
            }
        }
    }

    static void paintForBreadcrumbPanel(BreadcrumbPanel panel, OutlineController controller, OutlineSelection selection, Graphics g) {
        if (panel == null || controller == null || selection == null || g == null) return;
        final Icon icon = controller.getSelectionIcon();
        for (Component comp : panel.getComponents()) {
            if (comp instanceof NodeButton) {
                NodeButton btn = (NodeButton) comp;
                TreeNode node = btn.getNode();
                if (node != null && selection.isSelected(node)) {
                    Point p = controller.calculateSelectionIconPosition(comp.getBounds());
                    if (p != null) icon.paintIcon(panel, g, p.x, p.y);
                }
            }
        }
    }
}
