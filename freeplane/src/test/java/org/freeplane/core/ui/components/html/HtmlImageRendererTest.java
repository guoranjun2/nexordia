package org.freeplane.core.ui.components.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Rectangle;

import org.junit.Test;

public class HtmlImageRendererTest {
	@Test
	public void containsImageByWidthForWideImage() {
		final Rectangle bounds = new Rectangle(10, 20, 100, 50);

		final Rectangle imageBounds = HtmlImageRenderer.containedBounds(bounds, new Dimension(20, 10));

		assertThat(imageBounds).isEqualTo(new Rectangle(10, 20, 100, 50));
	}

	@Test
	public void containsImageByHeightForTallImage() {
		final Rectangle bounds = new Rectangle(10, 20, 100, 50);

		final Rectangle imageBounds = HtmlImageRenderer.containedBounds(bounds, new Dimension(10, 20));

		assertThat(imageBounds).isEqualTo(new Rectangle(47, 20, 25, 50));
	}
}
