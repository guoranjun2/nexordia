package org.freeplane.view.swing.map.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class RectangleCloudBoundsTest {
	@Test
	public void returnsEmptyRectangleWithoutContents() {
		Rectangle bounds = RectangleCloudBounds.fromContents(Collections.<Rectangle>emptyList(), 4, true, true);

		assertThat(bounds).isEqualTo(new Rectangle());
	}

	@Test
	public void enclosesAllContentRectangles() {
		Rectangle bounds = RectangleCloudBounds.fromContents(Arrays.asList(
				new Rectangle(20, 100, 30, 10),
				new Rectangle(5, 50, 5, 15),
				new Rectangle(40, 10, 20, 5)), 0, false, false);

		assertThat(bounds).isEqualTo(new Rectangle(5, 10, 55, 100));
	}

	@Test
	public void expandsOnlyRequestedAxes() {
		Rectangle bounds = RectangleCloudBounds.fromContents(Collections.singletonList(new Rectangle(10, 20, 30, 40)),
				5, true, false);

		assertThat(bounds).isEqualTo(new Rectangle(5, 20, 40, 40));
	}
}
