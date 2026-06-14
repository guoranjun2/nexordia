package org.freeplane.view.swing.map;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CoordinateAxisGridCalculatorTest {
	@Test
	public void keepsScreenGridStepInReadableRangeAcrossZoomLevels() {
		assertScreenStepIsReadable(32f);
		assertScreenStepIsReadable(1f);
		assertScreenStepIsReadable(0.1f);
		assertScreenStepIsReadable(0.001f);
	}

	@Test
	public void convertsScreenOffsetBackToStableMapCoordinate() {
		assertThat(CoordinateAxisGridCalculator.mapCoordinate(400d, 1f)).isEqualTo(400d);
		assertThat(CoordinateAxisGridCalculator.mapCoordinate(200d, 0.5f)).isEqualTo(400d);
		assertThat(CoordinateAxisGridCalculator.mapCoordinate(800d, 2f)).isEqualTo(400d);
	}

	private void assertScreenStepIsReadable(float zoom) {
		final double mapStep = CoordinateAxisGridCalculator.mapStep(zoom);
		assertThat(CoordinateAxisGridCalculator.screenStep(mapStep, zoom)).isBetween(60d, 160d);
	}
}
