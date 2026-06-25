package org.freeplane.core.ui.components.html;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

public final class HtmlImageViewSettings {
	private static final String MAXIMUM_IMAGE_WIDTH_PROPERTY =
			"org.freeplane.core.ui.components.html.maximumImageWidth";
	private static final String IMAGE_VIEW_ZOOM_PROPERTY =
			"org.freeplane.core.ui.components.html.imageViewZoom";
	static final String SOURCE_WIDTH_ATTRIBUTE = "src-width";
	static final String SOURCE_HEIGHT_ATTRIBUTE = "src-height";
	private static final int MINIMUM_WIDTH_TO_FIT = 100;

	public static void setMaximumImageWidth(Document document, int maximumWidth) {
		document.putProperty(MAXIMUM_IMAGE_WIDTH_PROPERTY, maximumWidth > 0 ? Integer.valueOf(maximumWidth) : null);
	}

	public static void setImageViewZoom(Document document, float zoom) {
		document.putProperty(IMAGE_VIEW_ZOOM_PROPERTY, zoom > 0 ? Float.valueOf(zoom) : null);
	}

	static Dimension fitToMaximumImageWidth(Document document, Dimension size, Dimension sourceSize) {
		final int maximumWidth = maximumImageWidth(document);
		if(maximumWidth > 0 && sourceSize != null)
			size = sourceSize;
		if(maximumWidth <= 0 || size.width <= maximumWidth || size.width <= MINIMUM_WIDTH_TO_FIT)
			return size;
		final int fittedWidth = Math.max(maximumWidth, MINIMUM_WIDTH_TO_FIT);
		return new Dimension(fittedWidth, Math.max(1, (int)Math.round(size.height * fittedWidth / (double)size.width)));
	}

	static boolean hasMaximumImageWidth(Document document) {
		return maximumImageWidth(document) > 0;
	}

	static Dimension sourceSize(Element element) {
		final int sourceWidth = intAttribute(element, SOURCE_WIDTH_ATTRIBUTE, -1);
		final int sourceHeight = intAttribute(element, SOURCE_HEIGHT_ATTRIBUTE, -1);
		if(sourceWidth > 0 && sourceHeight > 0)
			return new Dimension(sourceWidth, sourceHeight);
		return null;
	}

	public static void writeSourceSizeAttributes(HTMLDocument document) {
		final List<Element> imageElements = new ArrayList<>();
		collectImageElements(document.getDefaultRootElement(), imageElements);
		for(Element element : imageElements)
			writeSourceSizeAttributes(document, element);
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

	private static void collectImageElements(Element element, List<Element> imageElements) {
		if("img".equals(element.getName())) {
			imageElements.add(element);
			return;
		}
		for(int i = 0; i < element.getElementCount(); i++)
			collectImageElements(element.getElement(i), imageElements);
	}

	private static void writeSourceSizeAttributes(HTMLDocument document, Element element) {
		if(sourceSize(element) != null)
			return;
		final int width = intAttribute(element, HTML.Attribute.WIDTH, -1);
		final int height = intAttribute(element, HTML.Attribute.HEIGHT, -1);
		if(width <= 0 || height <= 0)
			return;
		final SimpleAttributeSet attributes = new SimpleAttributeSet();
		attributes.addAttribute(SOURCE_WIDTH_ATTRIBUTE, Integer.toString(width));
		attributes.addAttribute(SOURCE_HEIGHT_ATTRIBUTE, Integer.toString(height));
		document.setCharacterAttributes(element.getStartOffset(),
				element.getEndOffset() - element.getStartOffset(), attributes, false);
	}

	private static int intAttribute(Element element, Object attribute, int defaultValue) {
		final Object value = element.getAttributes().getAttribute(attribute);
		if(value == null)
			return defaultValue;
		try {
			return Integer.parseInt(value.toString());
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private HtmlImageViewSettings() {
	}
}
