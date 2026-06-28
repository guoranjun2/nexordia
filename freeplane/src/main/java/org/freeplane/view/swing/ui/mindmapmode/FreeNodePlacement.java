package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Dimension;
import java.awt.Point;

import org.freeplane.api.ChildrenSides;
import org.freeplane.features.map.NodeModel.Side;

final class FreeNodePlacement {
	static Placement fromMapPoint(Point mapPoint, Point sourceLocation, Dimension sourceSize, float zoom,
			boolean usesHorizontalLayout, ChildrenSides childrenSides) {
		final int primary = (int) (((usesHorizontalLayout ? mapPoint.y : mapPoint.x)
				- (usesHorizontalLayout ? sourceLocation.y : sourceLocation.x)) / zoom);
		final int secondary = (int) (((usesHorizontalLayout ? mapPoint.x : mapPoint.y)
				- (usesHorizontalLayout ? sourceLocation.x : sourceLocation.y)) / zoom);
		final int sourcePrimarySize = (int) ((usesHorizontalLayout ? sourceSize.height : sourceSize.width) / zoom);
		final boolean topOrLeft = primary < sourcePrimarySize / 2;
		final Side side = topOrLeft ? Side.TOP_OR_LEFT : Side.BOTTOM_OR_RIGHT;
		final Side layoutSide = layoutSide(childrenSides, side);
		final int hGap = layoutSide == Side.TOP_OR_LEFT ? sourcePrimarySize - primary : primary;
		return new Placement(new Point(hGap, secondary), side, layoutSide);
	}

	static Point centerCorrection(Side side, int deltaX, int deltaY, boolean usesHorizontalLayout) {
		if (usesHorizontalLayout) {
			final int hGap = side == Side.TOP_OR_LEFT ? -deltaY : deltaY;
			return new Point(hGap, deltaX);
		}
		final int hGap = side == Side.TOP_OR_LEFT ? -deltaX : deltaX;
		return new Point(hGap, deltaY);
	}

	private static Side layoutSide(ChildrenSides childrenSides, Side side) {
		switch (childrenSides) {
			case TOP_OR_LEFT:
				return Side.TOP_OR_LEFT;
			case BOTTOM_OR_RIGHT:
				return Side.BOTTOM_OR_RIGHT;
			default:
				return side;
		}
	}

	static final class Placement {
		private final Point point;
		private final Side side;
		private final Side layoutSide;

		private Placement(Point point, Side side, Side layoutSide) {
			this.point = point;
			this.side = side;
			this.layoutSide = layoutSide;
		}

		Point point() {
			return point;
		}

		Side side() {
			return side;
		}

		Side layoutSide() {
			return layoutSide;
		}
	}

	private FreeNodePlacement() {
	}
}
