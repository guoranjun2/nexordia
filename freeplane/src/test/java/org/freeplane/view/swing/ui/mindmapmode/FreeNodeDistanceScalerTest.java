package org.freeplane.view.swing.ui.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Point;

import org.junit.Test;

public class FreeNodeDistanceScalerTest {
	@Test
	public void pushesChildShiftFartherFromParentCenter() {
		Point scaled = FreeNodeDistanceScaler.scaleChildShiftFromParentCenter(new Dimension(100, 60),
				new Point(150, 90), new Dimension(40, 20), 1);

		assertThat(scaled.x).isGreaterThan(150);
		assertThat(scaled.y).isGreaterThan(90);
	}

	@Test
	public void inverseShiftScalingReturnsCloseToOriginalShift() {
		Point original = new Point(150, 90);
		Point pushed = FreeNodeDistanceScaler.scaleChildShiftFromParentCenter(new Dimension(100, 60),
				original, new Dimension(40, 20), 1);
		Point pulled = FreeNodeDistanceScaler.scaleChildShiftFromParentCenter(new Dimension(100, 60),
				pushed, new Dimension(40, 20), -1);

		assertThat(pulled.x).isCloseTo(original.x, org.assertj.core.data.Offset.offset(1));
		assertThat(pulled.y).isCloseTo(original.y, org.assertj.core.data.Offset.offset(1));
	}

	@Test
	public void pullsChildShiftCloserToParentCenter() {
		Point scaled = FreeNodeDistanceScaler.scaleChildShiftFromParentCenter(new Dimension(100, 60),
				new Point(150, 90), new Dimension(40, 20), -1);

		assertThat(scaled.x).isLessThan(150);
		assertThat(scaled.y).isLessThan(90);
	}
}
