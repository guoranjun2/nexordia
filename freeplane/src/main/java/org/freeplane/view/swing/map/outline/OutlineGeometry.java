package org.freeplane.view.swing.map.outline;

import java.awt.Dimension;

import javax.swing.JButton;
import java.awt.Insets;
import javax.swing.BorderFactory;

class OutlineGeometry {
    final int rowHeight;
    final int navButtonWidth;
    final int navButtonsTotalWidth;
    private final int indent;
    private final int standardGap;
    private final int buttonAreaWidth;
    final int iconDiameter;

    OutlineGeometry(JButton sampleButton) {

        sampleButton.setMargin(new Insets(0, 0, 0, 0));
        sampleButton.setFont(sampleButton.getFont().deriveFont(10f));
        sampleButton.setBorder(BorderFactory.createRaisedBevelBorder());


        final Dimension preferredButtonSize = sampleButton.getPreferredSize();
        this.rowHeight = Math.round(preferredButtonSize.height);
        this.indent = rowHeight;

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

    int calculateNavigationButtonBaseX(FlatNode flatNode) {
        return calculateNavigationButtonBaseX(flatNode.depth);
    }

}