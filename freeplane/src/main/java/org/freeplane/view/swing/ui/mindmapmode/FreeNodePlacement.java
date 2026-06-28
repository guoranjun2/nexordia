package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Dimension;
import java.awt.Point;

import org.freeplane.api.ChildrenSides;
import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.nodelocation.LocationModel;

final class FreeNodePlacement {
	static Placement fromMapPoint(Point mapPoint, Point sourceLocation, Dimension sourceSize, float zoom,
			boolean usesHorizontalLayout, ChildrenSides childrenSides) {
		final int primary = (int) (((usesHorizontalLayout ? mapPoint.y : mapPoint.x)
				- (usesHorizontalLayout ? sourceLocation.y : sourceLocation.x)) / zoom);
		final int secondary = (int) (((usesHorizontalLayout ? mapPoint.x : mapPoint.y)
				- (usesHorizontalLayout ? sourceLocation.x : sourceLocation.y)) / zoom);
		final int sourcePrimarySize = (int) ((usesHorizontalLayout ? sourceSize.height : sourceSize.width) / zoom);
		final Side side = side(primary, sourcePrimarySize);
		final Side layoutSide = layoutSide(childrenSides, side);
		final int hGap = layoutSide == Side.TOP_OR_LEFT ? sourcePrimarySize - primary : primary;
		return new Placement(new Point(hGap, secondary), side, layoutSide);
	}

	static Placement fromDrag(Point dragStartPoint, Point dragNextPoint, Point sourceLocation, Dimension sourceSize,
			float zoom, boolean usesHorizontalLayout, ChildrenSides childrenSides) {
		final int primary = (int) (((usesHorizontalLayout ? dragNextPoint.y : dragNextPoint.x)
				- (usesHorizontalLayout ? sourceLocation.y : sourceLocation.x)) / zoom);
		final int sourcePrimarySize = (int) ((usesHorizontalLayout ? sourceSize.height : sourceSize.width) / zoom);
		final Side side = side(primary, sourcePrimarySize);
		final Side layoutSide = layoutSide(childrenSides, side);
		final Location location = locationForDrag(LocationModel.DEFAULT_HGAP, LocationModel.DEFAULT_SHIFT_Y,
				dragStartPoint, dragNextPoint, zoom, layoutSide, usesHorizontalLayout);
		return new Placement(new Point(location.hGap().toBaseUnitsRounded(), location.shiftY().toBaseUnitsRounded()),
				side, layoutSide);
	}

	static Point centerCorrection(Side side, int deltaX, int deltaY, boolean usesHorizontalLayout) {
		if (usesHorizontalLayout) {
			final int hGap = side == Side.TOP_OR_LEFT ? -deltaY : deltaY;
			return new Point(hGap, deltaX);
		}
		final int hGap = side == Side.TOP_OR_LEFT ? -deltaX : deltaX;
		return new Point(hGap, deltaY);
	}

	static Location locationForCenter(Quantity<LengthUnit> hGap, Quantity<LengthUnit> shiftY, Side side, int deltaX,
			int deltaY, boolean usesHorizontalLayout) {
		final Point correction = centerCorrection(side, deltaX, deltaY, usesHorizontalLayout);
		return new Location(hGap.add(correction.x, LengthUnit.px), shiftY.add(correction.y, LengthUnit.px));
	}

	static Location locationForDrag(Quantity<LengthUnit> hGap, Quantity<LengthUnit> shiftY, Point dragStartPoint,
			Point dragNextPoint, float zoom, Side layoutSide, boolean usesHorizontalLayout) {
		final int hGapChange = hGapChange(dragStartPoint, dragNextPoint, zoom, layoutSide, usesHorizontalLayout);
		final int shiftYChange = shiftYChange(dragStartPoint, dragNextPoint, zoom, usesHorizontalLayout);
		return new Location(hGap.add(hGapChange, LengthUnit.px), shiftY.add(shiftYChange, LengthUnit.px));
	}

	static int hGapChange(Point dragStartPoint, Point dragNextPoint, float zoom, Side layoutSide,
			boolean usesHorizontalLayout) {
		final int distance = usesHorizontalLayout ? dragNextPoint.y - dragStartPoint.y
				: dragNextPoint.x - dragStartPoint.x;
		int hGapChange = (int) (distance / zoom);
		if (layoutSide == Side.TOP_OR_LEFT) {
			hGapChange = -hGapChange;
		}
		return hGapChange;
	}

	static int shiftYChange(Point dragStartPoint, Point dragNextPoint, float zoom, boolean usesHorizontalLayout) {
		final int distance = usesHorizontalLayout ? dragNextPoint.x - dragStartPoint.x
				: dragNextPoint.y - dragStartPoint.y;
		return (int) (distance / zoom);
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

	private static Side side(int primary, int sourcePrimarySize) {
		return primary < sourcePrimarySize / 2 ? Side.TOP_OR_LEFT : Side.BOTTOM_OR_RIGHT;
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

	static final class Location {
		private final Quantity<LengthUnit> hGap;
		private final Quantity<LengthUnit> shiftY;

		private Location(Quantity<LengthUnit> hGap, Quantity<LengthUnit> shiftY) {
			this.hGap = hGap;
			this.shiftY = shiftY;
		}

		Quantity<LengthUnit> hGap() {
			return hGap;
		}

		Quantity<LengthUnit> shiftY() {
			return shiftY;
		}
	}

	private FreeNodePlacement() {
	}
}
