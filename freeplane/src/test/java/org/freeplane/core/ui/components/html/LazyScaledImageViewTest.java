package org.freeplane.core.ui.components.html;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JLabel;
import javax.swing.text.View;

import org.junit.Test;

public class LazyScaledImageViewTest {
	@Test
	public void usesGraphicsTransformForTargetImageSize() {
		final BufferedImage canvas = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = canvas.createGraphics();
		try {
			graphics.scale(2.0, 0.5);

			final Dimension targetSize = LazyScaledImageView.targetSize(graphics, new Rectangle(0, 0, 12, 34));

			assertThat(targetSize).isEqualTo(new Dimension(24, 17));
		}
		finally {
			graphics.dispose();
		}
	}

	@Test
	public void usesHtmlDimensionsWithoutReadableImageSource() {
		final JLabel label = new JLabel();
		final ScaledHTML.Renderer renderer = ScaledHTML.createHTMLView(label,
				"<html><body><img src=\"file:/missing-image.png\" width=\"12\" height=\"34\"></body></html>");
		renderer.getPreferredSpan(View.X_AXIS);
		final LazyScaledImageView imageView = findImageView(renderer);

		assertThat(imageView).isNotNull();
		assertThat(imageView.getPreferredSpan(View.X_AXIS)).isEqualTo(12f);
		assertThat(imageView.getPreferredSpan(View.Y_AXIS)).isEqualTo(34f);
	}

	private LazyScaledImageView findImageView(View view) {
		if(view instanceof LazyScaledImageView)
			return (LazyScaledImageView)view;
		for(int i = 0; i < view.getViewCount(); i++) {
			final LazyScaledImageView imageView = findImageView(view.getView(i));
			if(imageView != null)
				return imageView;
		}
		return null;
	}
}
