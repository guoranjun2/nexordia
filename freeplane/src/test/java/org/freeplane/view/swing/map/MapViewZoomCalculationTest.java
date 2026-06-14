package org.freeplane.view.swing.map;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MapViewZoomCalculationTest {
	@Test
	public void increasesZoomByConfiguredShiftForNegativeWheelDirection() {
		assertThat(ZoomCalculator.calculate(1f, -1d, 0.04f)).isEqualTo(1.04f);
	}

	@Test
	public void decreasesZoomByConfiguredShiftForPositiveWheelDirection() {
		assertThat(ZoomCalculator.calculate(1f, 1d, 0.04f)).isEqualTo(0.96f);
	}

	@Test
	public void clampsZoomToUpperSupportedRange() {
		assertThat(ZoomCalculator.calculate(40f, -1d, 0.5f)).isEqualTo(32f);
	}
}
