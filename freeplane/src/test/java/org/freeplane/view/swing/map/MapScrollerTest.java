package org.freeplane.view.swing.map;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import org.junit.Test;

public class MapScrollerTest {
	@Test
	public void centersTinyZoomedContentWithoutTruncatingItsCenterPoint() {
		Rectangle rectangle = MapScroller.centeredVisibleRectangle(new Point(10, 20), new Dimension(1, 1),
				new Dimension(100, 80));

		assertThat(rectangle).isEqualTo(new Rectangle(-39, -19, 100, 80));
	}

	@Test
	public void centersRegularContentAgainstViewport() {
		Rectangle rectangle = MapScroller.centeredVisibleRectangle(new Point(30, 40), new Dimension(20, 10),
				new Dimension(120, 90));

		assertThat(rectangle).isEqualTo(new Rectangle(-20, 0, 120, 90));
	}

	@Test
	public void clampsExactCenterTargetToScrollableMapBounds() {
		Point target = MapScroller.clampedViewPosition(new Point(900, -40), new Dimension(1000, 800),
				new Dimension(300, 200));

		assertThat(target).isEqualTo(new Point(700, 0));
	}
}
