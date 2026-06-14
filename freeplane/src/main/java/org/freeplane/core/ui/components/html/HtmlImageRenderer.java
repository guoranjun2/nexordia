package org.freeplane.core.ui.components.html;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

public class HtmlImageRenderer {
	public static void paintContained(Graphics graphics, Rectangle bounds, URL base, String sourceText,
			Runnable repaintCallback) {
		final URL source = imageSource(base, sourceText);
		if (source == null) {
			return;
		}
		final String sourceKey = HtmlImageCache.sourceKey(source);
		final Dimension imageSize = HtmlImageCache.INSTANCE.getImageSizeOrSchedule(source, sourceKey, repaintCallback);
		if (imageSize == null) {
			return;
		}
		final Rectangle imageBounds = containedBounds(bounds, imageSize);
		final Dimension targetSize = targetSize(graphics, imageBounds);
		final BufferedImage image = HtmlImageCache.INSTANCE.getOrSchedule(source, sourceKey, targetSize.width,
				targetSize.height, repaintCallback);
		if (image != null) {
			graphics.drawImage(image, imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height, null);
		}
	}

	static Rectangle containedBounds(Rectangle bounds, Dimension imageSize) {
		final double scale = Math.min(bounds.width / (double) imageSize.width, bounds.height / (double) imageSize.height);
		final int width = Math.max(1, Math.min(bounds.width, (int) Math.ceil(imageSize.width * scale)));
		final int height = Math.max(1, Math.min(bounds.height, (int) Math.ceil(imageSize.height * scale)));
		return new Rectangle(bounds.x + (bounds.width - width) / 2, bounds.y + (bounds.height - height) / 2, width, height);
	}

	private static Dimension targetSize(Graphics graphics, Rectangle imageBounds) {
		if (! (graphics instanceof Graphics2D)) {
			return new Dimension(imageBounds.width, imageBounds.height);
		}
		final AffineTransform transform = ((Graphics2D) graphics).getTransform();
		final double scaleX = Math.abs(transform.getScaleX()) + Math.abs(transform.getShearY());
		final double scaleY = Math.abs(transform.getShearX()) + Math.abs(transform.getScaleY());
		return new Dimension(Math.max(1, (int) Math.ceil(imageBounds.width * scaleX)),
				Math.max(1, (int) Math.ceil(imageBounds.height * scaleY)));
	}

	private static URL imageSource(URL base, String sourceText) {
		try {
			return new URL(base, sourceText);
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	private HtmlImageRenderer() {
	}
}
