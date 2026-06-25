package org.freeplane.core.ui.components.html;

import java.awt.Dimension;

import javax.swing.text.Document;

public final class HtmlImageViewSettings {
	private static final String MAXIMUM_IMAGE_WIDTH_PROPERTY =
			"org.freeplane.core.ui.components.html.maximumImageWidth";
	private static final String IMAGE_VIEW_ZOOM_PROPERTY =
			"org.freeplane.core.ui.components.html.imageViewZoom";
	private static final int MINIMUM_WIDTH_TO_FIT = 100;

	public static void setMaximumImageWidth(Document document, int maximumWidth) {
		document.putProperty(MAXIMUM_IMAGE_WIDTH_PROPERTY, maximumWidth > 0 ? Integer.valueOf(maximumWidth) : null);
	}

	public static void setImageViewZoom(Document document, float zoom) {
		document.putProperty(IMAGE_VIEW_ZOOM_PROPERTY, zoom > 0 ? Float.valueOf(zoom) : null);
	}

	static Dimension fitToMaximumImageWidth(Document document, Dimension size) {
		final int maximumWidth = maximumImageWidth(document);
		if(maximumWidth <= 0 || size.width <= maximumWidth || size.width <= MINIMUM_WIDTH_TO_FIT)
			return size;
		return new Dimension(maximumWidth, Math.max(1, (int)Math.round(size.height * maximumWidth / (double)size.width)));
	}

	static float imageViewZoom(Document document, float defaultZoom) {
		final Object value = document.getProperty(IMAGE_VIEW_ZOOM_PROPERTY);
		if(value instanceof Number) {
			final float zoom = ((Number)value).floatValue();
			if(zoom > 0)
				return zoom;
		}
		return defaultZoom;
	}

	private static int maximumImageWidth(Document document) {
		final Object value = document.getProperty(MAXIMUM_IMAGE_WIDTH_PROPERTY);
		if(value instanceof Number)
			return Math.max(0, ((Number)value).intValue());
		return 0;
	}

	private HtmlImageViewSettings() {
	}
}
