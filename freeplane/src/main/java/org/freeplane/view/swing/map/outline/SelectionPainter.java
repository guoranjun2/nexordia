package org.freeplane.view.swing.map.outline;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

final class SelectionPainter {
    private SelectionPainter() {}

    static void paintForBlockPanel(BlockPanel panel, ScrollableTreePanel parentPanel, OutlineSelection selection, Graphics g) {
        if (panel == null || parentPanel == null || selection == null || g == null) return;
        for (Component comp : panel.getComponents()) {
            if (comp instanceof NodeButton) {
                NodeButton btn = (NodeButton) comp;
                TreeNode node = btn.getNode();
                if (node != null && selection.isSelected(node)) {
                    if (! parentPanel.isNodeInBreadcrumbArea(node)) {
                    	underlineSelection(g, panel, btn);
                    }
                }
            }
        }
    }

	private static void underlineSelection(Graphics g, Component panel, Component btn) {
		g.setColor(Color.BLUE);
		((Graphics2D)g).setStroke(new BasicStroke(3));
		final int y1 = btn.getY() - 1;
		g.drawLine(0, y1, panel.getWidth(), y1);
		final int y2 = y1 + OutlineGeometry.getInstance().rowHeight;
		g.drawLine(0, y2, panel.getWidth(), y2);
	}

    static void paintForBreadcrumbPanel(BreadcrumbPanel panel, OutlineController controller, OutlineSelection selection, Graphics g) {
        if (panel == null || controller == null || selection == null || g == null) return;
        for (Component comp : panel.getComponents()) {
            if (comp instanceof NodeButton) {
                NodeButton btn = (NodeButton) comp;
                TreeNode node = btn.getNode();
                if (node != null && selection.isSelected(node)) {
                	underlineSelection(g, panel, btn);
                }
            }
        }
    }
}
