package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Dimension;
import java.awt.geom.Point2D;

final class FreeNodeRotator {
	static Point2D.Double rotateChildShiftAroundParentCenter(Dimension parentSize, Point2D.Double childShift,
			Dimension childSize,
			double degrees) {
		final double radians = Math.toRadians(degrees);
		final double cos = Math.cos(radians);
		final double sin = Math.sin(radians);
		final double parentCenterX = parentSize.width / 2d;
		final double parentCenterY = parentSize.height / 2d;
		final double childCenterX = childShift.x + childSize.width / 2d;
		final double childCenterY = childShift.y + childSize.height / 2d;
		final double dx = childCenterX - parentCenterX;
		final double dy = childCenterY - parentCenterY;
		final double newChildCenterX = parentCenterX + dx * cos - dy * sin;
		final double newChildCenterY = parentCenterY + dx * sin + dy * cos;
		return new Point2D.Double(newChildCenterX - childSize.width / 2d,
				newChildCenterY - childSize.height / 2d);
	}

	private FreeNodeRotator() {
	}
}
