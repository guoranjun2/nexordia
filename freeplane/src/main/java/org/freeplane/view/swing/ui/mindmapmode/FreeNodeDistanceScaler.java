package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Dimension;
import java.awt.Point;

final class FreeNodeDistanceScaler {
	private static final double SCALE_STEP = 0.08d;

	static Point scaleChildShiftFromParentCenter(Dimension parentSize, Point childShift, Dimension childSize,
			double direction) {
		final double factor = scaleFactor(direction);
		final double parentCenterX = parentSize.width / 2d;
		final double parentCenterY = parentSize.height / 2d;
		final double childCenterX = childShift.x + childSize.width / 2d;
		final double childCenterY = childShift.y + childSize.height / 2d;
		final double newChildCenterX = parentCenterX + (childCenterX - parentCenterX) * factor;
		final double newChildCenterY = parentCenterY + (childCenterY - parentCenterY) * factor;
		return new Point((int) Math.round(newChildCenterX - childSize.width / 2d),
				(int) Math.round(newChildCenterY - childSize.height / 2d));
	}

	private static double scaleFactor(double direction) {
		return Math.pow(1d + SCALE_STEP, direction);
	}

	private FreeNodeDistanceScaler() {
	}
}
