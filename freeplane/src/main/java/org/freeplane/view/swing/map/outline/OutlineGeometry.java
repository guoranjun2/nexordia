package org.freeplane.view.swing.map.outline;

import java.awt.Dimension;

import javax.swing.JButton;

class OutlineGeometry {
	private final int arrowAreaWidth;
    final int indent;
    final int rowHeight;
    final int navButtonWidth;
    final int navButtonsTotalWidth;
    private final int standardGap;
    private final int buttonAreaWidth;
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
        this.rowHeight = Math.round(preferredButtonSize.height);

        this.navButtonWidth = Math.round(preferredButtonSize.width * 20 / 13);
        this.navButtonsTotalWidth = 3 * navButtonWidth;
        this.standardGap = Math.round(preferredButtonSize.width * 12 / 13);
        this.buttonAreaWidth = navButtonsTotalWidth + (2 * standardGap);
        this.iconDiameter = Math.round(preferredButtonSize.width * 10 / 13);
    }

    int calculateTextButtonX(int depth) {
        if (depth == 0) {
            return buttonAreaWidth - indent;
        } else {
            return (depth * indent) + buttonAreaWidth - indent;
        }
    }

    private int calculateNavigationButtonBaseX(int depth) {
        int textButtonX = calculateTextButtonX(depth);
        return Math.max(0, textButtonX - navButtonsTotalWidth);
    }

    public int calculateNavigationButtonBaseX(FlatNode flatNode) {
        return calculateNavigationButtonBaseX(flatNode.depth);
    }

}