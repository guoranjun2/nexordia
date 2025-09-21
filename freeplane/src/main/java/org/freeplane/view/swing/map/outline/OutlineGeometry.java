package org.freeplane.view.swing.map.outline;

import java.awt.Dimension;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.UITools;

class OutlineGeometry {
	private static OutlineGeometry INSTANCE;
	static OutlineGeometry getInstance() {return INSTANCE;}
    final int rowHeight;
    final int navButtonWidth;
    private final int indent;
    final int iconDiameter;
	static final float itemFontSize() {
		return UITools.FONT_SCALE_FACTOR * (float) ResourceController.getResourceController().getDoubleProperty("outlineItemFontSize", 8f);
	}
	public static float buttonFontSize() {
		return itemFontSize() * 5 / 4;
	}

	static {
		INSTANCE = new OutlineGeometry();
		final ResourceController resourceController = ResourceController.getResourceController();
		resourceController.setDefaultProperty("outlineItemIndentation",
				asLength(INSTANCE.rowHeight).toString());
		resourceController.addPropertyChangeListener(OutlineGeometry::updateGeometry);
	}

	private static Quantity<LengthUnit> asLength(int rowHeight) {
		return new Quantity<>(rowHeight, LengthUnit.px).in(LengthUnit.mm);
	}

	@SuppressWarnings("unused")
	private static void updateGeometry(String propertyName, String newValue, String oldValue) {
		if(propertyName.equals("outlineItemFontSize")
				|| propertyName.equals("outlineItemIndentation")
				|| propertyName.equals("showOutlineFoldingButtons"))
			INSTANCE = new OutlineGeometry();
	}

    private OutlineGeometry() {
    	JButton sampleButton = new JButton("▶");
        sampleButton.setMargin(new Insets(0, 0, 0, 0));
        sampleButton.setFont(sampleButton.getFont().deriveFont(buttonFontSize()));
        sampleButton.setBorder(BorderFactory.createRaisedBevelBorder());

        final Dimension preferredButtonSize = sampleButton.getPreferredSize();
        this.rowHeight = Math.round(preferredButtonSize.height);
        final ResourceController resourceController = ResourceController.getResourceController();
		final Quantity<LengthUnit> indentQuantity = resourceController.getLengthQuantityProperty("outlineItemIndentation");
		this.indent = indentQuantity != null ? indentQuantity.toBaseUnitsRounded() : rowHeight;

		if(resourceController.getBooleanProperty("showOutlineFoldingButtons", true))
			this.navButtonWidth = Math.round(preferredButtonSize.width * 20 / 13);
		else
			this.navButtonWidth = 0;
        this.iconDiameter = Math.round(preferredButtonSize.width * 10 / 13);
    }

    int calculateNodeButtonX(int level) {
    	if(navButtonWidth == 0)
    		return level * indent;
    	else
    		return level == 0 ? 2 * navButtonWidth : (level - 1) * indent + 3 * navButtonWidth;
    }

	int calculateNavigationButtonX(final int level) {
		final int baseX = level == 0 ? -navButtonWidth : (level - 1) * indent;
		return baseX;
	}
}
