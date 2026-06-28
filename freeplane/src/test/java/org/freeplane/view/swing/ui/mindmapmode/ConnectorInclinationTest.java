package org.freeplane.view.swing.ui.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Point;
import java.awt.geom.Rectangle2D;

import org.junit.Test;

public class ConnectorInclinationTest {
	@Test
	public void createsHorizontalInclinationForRightSideTarget() {
		ConnectorInclination.Inclination inclination = ConnectorInclination.calculate(
				rect(0, 0, 100, 40), rect(300, 20, 100, 40), false, false, ConnectorInclination.AUTO);

		assertThat(inclination.start()).isEqualTo(new Point(100, 0));
		assertThat(inclination.end()).isEqualTo(new Point(-100, 0));
	}

	@Test
	public void keepsPositiveHorizontalInclinationForLeftSideNodes() {
		ConnectorInclination.Inclination inclination = ConnectorInclination.calculate(
				rect(300, 20, 100, 40), rect(0, 0, 100, 40), true, true, ConnectorInclination.AUTO);

		assertThat(inclination.start()).isEqualTo(new Point(100, 0));
		assertThat(inclination.end()).isEqualTo(new Point(-100, 0));
	}

	@Test
	public void createsVerticalInclinationForStackedTarget() {
		ConnectorInclination.Inclination inclination = ConnectorInclination.calculate(
				rect(0, 0, 100, 40), rect(20, 300, 100, 40), false, false, ConnectorInclination.AUTO);

		assertThat(inclination.start()).isEqualTo(new Point(0, 120));
		assertThat(inclination.end()).isEqualTo(new Point(0, -120));
	}

	private Rectangle2D rect(double x, double y, double width, double height) {
		return new Rectangle2D.Double(x, y, width, height);
	}
}
