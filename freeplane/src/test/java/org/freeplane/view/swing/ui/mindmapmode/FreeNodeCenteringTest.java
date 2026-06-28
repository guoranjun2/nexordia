package org.freeplane.view.swing.ui.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Point;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.features.map.NodeModel.Side;
import org.junit.Test;

public class FreeNodeCenteringTest {
	@Test
	public void movesRightSideFreeNodeCenterToRequestedMapPoint() {
		FreeNodePlacement.Location location = FreeNodeCentering.locationForCenter(
				new Quantity<LengthUnit>(100, LengthUnit.px),
				new Quantity<LengthUnit>(30, LengthUnit.px),
				new Point(200, 100), new Point(260, 140), 2f, Side.BOTTOM_OR_RIGHT, false);

		assertThat(location.hGap().toBaseUnitsRounded()).isEqualTo(130);
		assertThat(location.shiftY().toBaseUnitsRounded()).isEqualTo(50);
	}

	@Test
	public void movesTopSideHorizontalFreeNodeCenterToRequestedMapPoint() {
		FreeNodePlacement.Location location = FreeNodeCentering.locationForCenter(
				new Quantity<LengthUnit>(80, LengthUnit.px),
				new Quantity<LengthUnit>(25, LengthUnit.px),
				new Point(200, 100), new Point(260, 140), 2f, Side.TOP_OR_LEFT, true);

		assertThat(location.hGap().toBaseUnitsRounded()).isEqualTo(60);
		assertThat(location.shiftY().toBaseUnitsRounded()).isEqualTo(55);
	}
}
