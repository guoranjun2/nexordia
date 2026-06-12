package org.freeplane.view.swing.map.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class CircleCloudBoundsTest {
	@Test
	public void returnsEmptyCircleWithoutCenter() {
		Ellipse2D.Double circle = CircleCloudBounds.fromCenterAndDescendants(null,
				Collections.<Rectangle2D.Double>emptyList(), 4);

		assertThat(circle.width).isEqualTo(0);
		assertThat(circle.height).isEqualTo(0);
	}

	@Test
	public void usesCenterAndPaddingWithoutDescendants() {
		Ellipse2D.Double circle = CircleCloudBounds.fromCenterAndDescendants(new Point2D.Double(10, 20),
				Collections.<Rectangle2D.Double>emptyList(), 5);

		assertThat(circle.x).isEqualTo(5);
		assertThat(circle.y).isEqualTo(15);
		assertThat(circle.width).isEqualTo(10);
		assertThat(circle.height).isEqualTo(10);
	}

	@Test
	public void usesCloudNodeAsCenterAndMostDistantDescendantCornerAsRadius() {
		Ellipse2D.Double circle = CircleCloudBounds.fromCenterAndDescendants(new Point2D.Double(0, 0), Arrays.asList(
				new Rectangle2D.Double(6, 8, 1, 1),
				new Rectangle2D.Double(3, 2, 1, 1)), 2);

		assertThat(circle.x).isEqualTo(-Math.sqrt(130) - 2);
		assertThat(circle.y).isEqualTo(-Math.sqrt(130) - 2);
		assertThat(circle.width).isEqualTo(2 * (Math.sqrt(130) + 2));
		assertThat(circle.height).isEqualTo(2 * (Math.sqrt(130) + 2));
	}

	@Test
	public void includesWideDescendantNodeBounds() {
		Ellipse2D.Double circle = CircleCloudBounds.fromCenterAndDescendants(new Point2D.Double(0, 0), Arrays.asList(
				new Rectangle2D.Double(10, -5, 30, 10)), 2);

		assertThat(circle.getMaxX()).isGreaterThan(40);
	}

}
