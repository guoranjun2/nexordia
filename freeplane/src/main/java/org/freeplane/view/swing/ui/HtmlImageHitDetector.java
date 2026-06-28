package org.freeplane.view.swing.ui;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.Element;
import javax.swing.text.View;
import javax.swing.text.html.HTML;

import org.freeplane.features.image.ImageHtmlInserter;
import org.freeplane.features.image.ImageHtmlInserter.ImageSize;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.ZoomableLabel;

public class HtmlImageHitDetector {
	public static class Hit {
		private final int imageIndex;
		private final String source;
		private final Rectangle bounds;

		Hit(final int imageIndex, final String source, final Rectangle bounds) {
			this.imageIndex = imageIndex;
			this.source = source;
			this.bounds = bounds;
		}

		public int getImageIndex() {
			return imageIndex;
		}

		public String getSource() {
			return source;
		}

		public Rectangle getBounds() {
			return bounds;
		}
	}

	private final ImageHtmlInserter htmlInserter = new ImageHtmlInserter();

	public Hit findImageAt(final ZoomableLabel label, final Point point) {
		return findImageNear(label, point, 0, label.getText());
	}

	public Hit findImageAt(final ZoomableLabel label, final Point point, final String html) {
		return findImageNear(label, point, 0, html);
	}

	public Hit findImageNear(final ZoomableLabel label, final Point point, final int margin) {
		return findImageNear(label, point, margin, label.getText());
	}

	public Hit findImageNear(final ZoomableLabel label, final Point point, final int margin, final String html) {
		final View renderer = (View) label.getClientProperty(BasicHTML.propertyKey);
		final Rectangle textBounds = label.getUI().getTextR(label);
		if (textBounds == null) {
			return null;
		}
		if (renderer != null) {
			final Hit hit = findImageNear(renderer, textBounds, zoom(label), point, margin);
			if (hit != null) {
				return hitWithSourceFromHtml(hit, html);
			}
		}
		return findImageFromHtml(label, point, margin, textBounds, html);
	}

	private Hit findImageNear(final View renderer, final Rectangle zoomedTextBounds, final float zoom,
			final Point point, final int margin) {
		final Rectangle textBounds = unzoom(zoomedTextBounds, zoom);
		final ImageCounter counter = new ImageCounter();
		return findImageNear(renderer, textBounds, zoom, point, margin, counter);
	}

	private Hit findImageNear(final View view, final Shape allocation, final float zoom, final Point point,
			final int margin, final ImageCounter counter) {
		if (allocation == null) {
			return null;
		}
		final Element element = view.getElement();
		if (element != null && "img".equals(element.getName())) {
			final int imageIndex = counter.next();
			final Rectangle bounds = zoom(allocation.getBounds(), zoom);
			return contains(bounds, point, margin) ? new Hit(imageIndex, imageSource(element), bounds) : null;
		}
		for (int i = 0; i < view.getViewCount(); i++) {
			final Hit hit = findImageNear(view.getView(i), view.getChildAllocation(i, allocation), zoom, point,
					margin, counter);
			if (hit != null) {
				return hit;
			}
		}
		return null;
	}

	private Hit findImageFromHtml(final ZoomableLabel label, final Point point, final int margin,
			final Rectangle textBounds, final String html) {
		final ImageSize imageSize = htmlInserter.firstImageSize(html);
		if (imageSize == null) {
			return null;
		}
		final float zoom = zoom(label);
		final Rectangle bounds = new Rectangle(textBounds.x, textBounds.y,
				Math.max(1, Math.round(imageSize.getWidth() * zoom)),
				Math.max(1, Math.round(imageSize.getHeight() * zoom)));
		return contains(bounds, point, margin)
				? new Hit(0, htmlInserter.imageSource(html, 0), bounds)
				: null;
	}

	private Hit hitWithSourceFromHtml(final Hit hit, final String html) {
		final String source = htmlInserter.imageSource(html, hit.getImageIndex());
		return source != null ? new Hit(hit.getImageIndex(), source, hit.getBounds()) : hit;
	}

	private boolean contains(final Rectangle bounds, final Point point, final int margin) {
		final Rectangle sensitiveBounds = new Rectangle(bounds);
		sensitiveBounds.grow(margin, margin);
		return sensitiveBounds.contains(point);
	}

	private Rectangle unzoom(final Rectangle bounds, final float zoom) {
		if (zoom == 1f) {
			return new Rectangle(bounds);
		}
		return new Rectangle(Math.round(bounds.x / zoom), Math.round(bounds.y / zoom),
				Math.round(bounds.width / zoom), Math.round(bounds.height / zoom));
	}

	private Rectangle zoom(final Rectangle bounds, final float zoom) {
		if (zoom == 1f) {
			return new Rectangle(bounds);
		}
		return new Rectangle(Math.round(bounds.x * zoom), Math.round(bounds.y * zoom),
				Math.max(1, Math.round(bounds.width * zoom)), Math.max(1, Math.round(bounds.height * zoom)));
	}

	private String imageSource(final Element element) {
		final Object source = element.getAttributes().getAttribute(HTML.Attribute.SRC);
		return source != null ? source.toString() : "";
	}

	private float zoom(final ZoomableLabel label) {
		final NodeView nodeView = label.getNodeView();
		return nodeView != null ? nodeView.getMap().getZoom() : 1f;
	}

	private static class ImageCounter {
		private int value;

		int next() {
			return value++;
		}
	}
}
