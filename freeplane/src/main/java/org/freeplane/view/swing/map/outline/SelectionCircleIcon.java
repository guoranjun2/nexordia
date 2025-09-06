
package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

class SelectionCircleIcon implements Icon {
    private final int diameter;
    private final Color color;

    SelectionCircleIcon(Color color, int diameter) {
        this.color = color;
        this.diameter = diameter;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(color);
        g2d.fillOval(x, y, diameter, diameter);
        g2d.dispose();
    }

    @Override
    public int getIconWidth() {
        return diameter;
    }

    @Override
    public int getIconHeight() {
        return diameter;
    }
}