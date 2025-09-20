package org.freeplane.view.swing.map.outline;

import java.awt.Dimension;

import javax.swing.JButton;

import org.freeplane.core.ui.components.UITools;

import java.awt.Insets;
import javax.swing.BorderFactory;

class OutlineGeometry {
    final int rowHeight;
    final int navButtonWidth;
    private final int indent;
    final int iconDiameter;

    OutlineGeometry(JButton sampleButton) {

        sampleButton.setMargin(new Insets(0, 0, 0, 0));
        sampleButton.setFont(sampleButton.getFont().deriveFont(UITools.FONT_SCALE_FACTOR * 10f));
        sampleButton.setBorder(BorderFactory.createRaisedBevelBorder());


        final Dimension preferredButtonSize = sampleButton.getPreferredSize();
        this.rowHeight = Math.round(preferredButtonSize.height);
        this.indent = rowHeight;

        this.navButtonWidth = Math.round(preferredButtonSize.width * 20 / 13);
        this.iconDiameter = Math.round(preferredButtonSize.width * 10 / 13);
    }

    int calculateNodeButtonX(int level) {
        return level == 0 ? 2 * navButtonWidth : (level - 1) * indent + 3 * navButtonWidth;
    }

	int calculateNavigationButtonX(final int level) {
		final int baseX = level == 0 ? -navButtonWidth : (level - 1) * indent;
		return baseX;
	}

}
