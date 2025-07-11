package org.freeplane.view.swing.map.outline;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JButton;

class OutlineGeometry {
    final int arrowAreaWidth;
    final int indent;
    final int rowHeight;
    final int navButtonWidth;
    final int navButtonsTotalWidth;
    final int standardGap;
    final int buttonAreaWidth;
    final int iconDiameter;
    
    public OutlineGeometry(JButton sampleButton) {
        // Configure the sample button exactly like navigation buttons
        sampleButton.setMargin(new java.awt.Insets(0, 0, 0, 0));
        sampleButton.setFont(sampleButton.getFont().deriveFont(10f));
        sampleButton.setBorder(javax.swing.BorderFactory.createRaisedBevelBorder());
        
        // Use exact original calculations with multipliers
        final Dimension preferredButtonSize = sampleButton.getPreferredSize();
        this.arrowAreaWidth = Math.round(preferredButtonSize.width * 60 / 13);
        this.indent = arrowAreaWidth / 2;
        this.rowHeight = Math.round(preferredButtonSize.height * 30 / 17);
        
        this.navButtonWidth = Math.round(preferredButtonSize.width * 20 / 13);
        this.navButtonsTotalWidth = 3 * navButtonWidth;
        this.standardGap = Math.round(preferredButtonSize.width * 12 / 13);
        this.buttonAreaWidth = navButtonsTotalWidth + (2 * standardGap);
        this.iconDiameter = Math.round(preferredButtonSize.width * 10 / 13);
    }
    
    public int calculateTextButtonX(int depth) {
        if (depth == 0) {
            return buttonAreaWidth - indent;
        } else {
            return (depth * indent) + buttonAreaWidth - indent;
        }
    }
    
    public int calculateNavigationButtonBaseX(int depth) {
        int textButtonX = calculateTextButtonX(depth);
        return Math.max(0, textButtonX - navButtonsTotalWidth);
    }
    
    public int calculateNavigationButtonBaseX(FlatNode flatNode) {
        return calculateNavigationButtonBaseX(flatNode.depth);
    }
    
    public Rectangle calculateNodeBounds(int displayIndex, int breadcrumbAreaHeight, int panelWidth) {
        int y = breadcrumbAreaHeight + displayIndex * rowHeight;
        return new Rectangle(0, y, panelWidth, rowHeight);
    }
    
    public Point calculateButtonPosition(int baseX, int buttonIndex, int y) {
        int x = baseX + (buttonIndex * navButtonWidth);
        return new Point(x, y);
    }
    
    public Dimension calculateButtonSize() {
        return new Dimension(navButtonWidth, rowHeight);
    }
    
    public int calculateRequiredWidth(int maxDepth) {
        return calculateTextButtonX(maxDepth) + 200; // Extra space for text
    }
    
    public int calculateTotalHeight(int visibleNodeCount, int breadcrumbAreaHeight) {
        return breadcrumbAreaHeight + visibleNodeCount * rowHeight;
    }
} 