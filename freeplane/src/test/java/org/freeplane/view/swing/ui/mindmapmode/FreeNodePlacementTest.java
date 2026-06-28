package org.freeplane.view.swing.ui.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Point;

import org.freeplane.api.ChildrenSides;
import org.freeplane.features.map.NodeModel.Side;
import org.junit.Test;

public class FreeNodePlacementTest {
	@Test
	public void placesRightSidePointRelativeToRoot() {
		FreeNodePlacement.Placement placement = FreeNodePlacement.fromMapPoint(
				new Point(300, 190), new Point(100, 50), new Dimension(120, 80), 2f, false, ChildrenSides.BOTH_SIDES);

		assertThat(placement.point()).isEqualTo(new Point(100, 70));
		assertThat(placement.side()).isEqualTo(Side.BOTTOM_OR_RIGHT);
		assertThat(placement.layoutSide()).isEqualTo(Side.BOTTOM_OR_RIGHT);
	}

	@Test
	public void mirrorsLeftSideHorizontalDistanceFromRoot() {
		FreeNodePlacement.Placement placement = FreeNodePlacement.fromMapPoint(
				new Point(120, 190), new Point(100, 50), new Dimension(120, 80), 2f, false, ChildrenSides.BOTH_SIDES);

		assertThat(placement.point()).isEqualTo(new Point(50, 70));
		assertThat(placement.side()).isEqualTo(Side.TOP_OR_LEFT);
		assertThat(placement.layoutSide()).isEqualTo(Side.TOP_OR_LEFT);
	}

	@Test
	public void placesTopSidePointForHorizontalLayout() {
		FreeNodePlacement.Placement placement = FreeNodePlacement.fromMapPoint(
				new Point(160, 70), new Point(100, 50), new Dimension(120, 80), 2f, true, ChildrenSides.BOTH_SIDES);

		assertThat(placement.point()).isEqualTo(new Point(30, 30));
		assertThat(placement.side()).isEqualTo(Side.TOP_OR_LEFT);
		assertThat(placement.layoutSide()).isEqualTo(Side.TOP_OR_LEFT);
	}

	@Test
	public void placesBottomSidePointForHorizontalLayout() {
		FreeNodePlacement.Placement placement = FreeNodePlacement.fromMapPoint(
				new Point(160, 150), new Point(100, 50), new Dimension(120, 80), 2f, true, ChildrenSides.BOTH_SIDES);

		assertThat(placement.point()).isEqualTo(new Point(50, 30));
		assertThat(placement.side()).isEqualTo(Side.BOTTOM_OR_RIGHT);
		assertThat(placement.layoutSide()).isEqualTo(Side.BOTTOM_OR_RIGHT);
	}

	@Test
	public void keepsLeftDropPositionWhenTopToBottomParentForcesRightSide() {
		FreeNodePlacement.Placement placement = FreeNodePlacement.fromMapPoint(
				new Point(80, 190), new Point(100, 50), new Dimension(120, 80), 1f, false, ChildrenSides.BOTTOM_OR_RIGHT);

		assertThat(placement.point()).isEqualTo(new Point(-20, 140));
		assertThat(placement.side()).isEqualTo(Side.TOP_OR_LEFT);
		assertThat(placement.layoutSide()).isEqualTo(Side.BOTTOM_OR_RIGHT);
	}

	@Test
	public void keepsRightDropPositionWhenTopToBottomParentForcesLeftSide() {
		FreeNodePlacement.Placement placement = FreeNodePlacement.fromMapPoint(
				new Point(260, 190), new Point(100, 50), new Dimension(120, 80), 1f, false, ChildrenSides.TOP_OR_LEFT);

		assertThat(placement.point()).isEqualTo(new Point(-40, 140));
		assertThat(placement.side()).isEqualTo(Side.BOTTOM_OR_RIGHT);
		assertThat(placement.layoutSide()).isEqualTo(Side.TOP_OR_LEFT);
	}

	@Test
	public void correctsCenterForRightAndLeftNodes() {
		assertThat(FreeNodePlacement.centerCorrection(Side.TOP_OR_LEFT, 12, 7, false)).isEqualTo(new Point(-12, 7));
		assertThat(FreeNodePlacement.centerCorrection(Side.BOTTOM_OR_RIGHT, 12, 7, false)).isEqualTo(new Point(12, 7));
	}

	@Test
	public void correctsCenterForTopAndBottomNodes() {
		assertThat(FreeNodePlacement.centerCorrection(Side.TOP_OR_LEFT, 12, 7, true)).isEqualTo(new Point(-7, 12));
		assertThat(FreeNodePlacement.centerCorrection(Side.BOTTOM_OR_RIGHT, 12, 7, true)).isEqualTo(new Point(7, 12));
	}

}
