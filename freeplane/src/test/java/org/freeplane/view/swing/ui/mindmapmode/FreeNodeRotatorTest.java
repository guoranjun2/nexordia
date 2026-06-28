package org.freeplane.view.swing.ui.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.geom.Point2D;

import org.assertj.core.data.Offset;
import org.junit.Test;

public class FreeNodeRotatorTest {
	@Test
	public void rotatesClockwiseInScreenCoordinates() {
		Point2D.Double rotated = FreeNodeRotator.rotateChildShiftAroundParentCenter(new Dimension(100, 100),
				new Point2D.Double(90, 40), new Dimension(20, 20), 90);

		assertThat(rotated.x).isCloseTo(40, Offset.offset(0.0001));
		assertThat(rotated.y).isCloseTo(90, Offset.offset(0.0001));
	}

	@Test
	public void rotatesCounterClockwiseInScreenCoordinates() {
		Point2D.Double rotated = FreeNodeRotator.rotateChildShiftAroundParentCenter(new Dimension(100, 100),
				new Point2D.Double(90, 40), new Dimension(20, 20), -90);

		assertThat(rotated.x).isCloseTo(40, Offset.offset(0.0001));
		assertThat(rotated.y).isCloseTo(-10, Offset.offset(0.0001));
	}

	@Test
	public void inverseRotationReturnsCloseToOriginalShift() {
		Point2D.Double original = new Point2D.Double(130, 85);
		Point2D.Double rotated = FreeNodeRotator.rotateChildShiftAroundParentCenter(new Dimension(100, 80),
				original, new Dimension(30, 20), 37);
		Point2D.Double restored = FreeNodeRotator.rotateChildShiftAroundParentCenter(new Dimension(100, 80),
				rotated, new Dimension(30, 20), -37);

		assertThat(restored.x).isCloseTo(original.x, Offset.offset(1d));
		assertThat(restored.y).isCloseTo(original.y, Offset.offset(1d));
	}

	@Test
	public void keepsSubPixelRotationInsteadOfRoundingItAway() {
		Point2D.Double rotated = FreeNodeRotator.rotateChildShiftAroundParentCenter(new Dimension(100, 80),
				new Point2D.Double(130, 85), new Dimension(30, 20), 0.2);

		assertThat(rotated.x).isNotEqualTo(Math.rint(rotated.x));
		assertThat(rotated.y).isNotEqualTo(Math.rint(rotated.y));
	}
}
