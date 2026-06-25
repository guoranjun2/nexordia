package org.freeplane.core.ui.components.html;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.ImageObserver;

import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.ImageView;

import org.freeplane.features.mode.Controller;

class ScaledGifImageView extends ImageView {
	private final ImageObserver imageObserver = new ScaledImageObserver();
	private Rectangle repaintBounds;

	ScaledGifImageView(Element elem) {
		super(elem);
	}

	@Override
	public float getPreferredSpan(int axis) {
		final Dimension size = HtmlImageViewSettings.fitToMaximumImageWidth(getDocument(),
				new Dimension(Math.max(1, (int)super.getPreferredSpan(X_AXIS)),
						Math.max(1, (int)super.getPreferredSpan(Y_AXIS))));
		if(axis == X_AXIS)
			return size.width;
		if(axis == Y_AXIS)
			return size.height;
		return super.getPreferredSpan(axis);
	}

	@Override
	public void paint(Graphics graphics, Shape allocation) {
		final Image image = getImage();
		if(image == null) {
			super.paint(graphics, allocation);
			return;
		}
		final Rectangle bounds = allocation instanceof Rectangle ? (Rectangle) allocation : allocation.getBounds();
		final Rectangle imageBounds = imageBounds(bounds);
		repaintBounds = scaledBounds(bounds, zoom());
		if(! (graphics instanceof Graphics2D)) {
			graphics.drawImage(image, imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height,
					imageObserver);
			return;
		}
		final Graphics2D graphics2D = (Graphics2D) graphics.create();
		try {
			graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics2D.drawImage(image, imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height,
					imageObserver);
		}
		finally {
			graphics2D.dispose();
		}
	}

	private Rectangle imageBounds(Rectangle bounds) {
		final int border = intAttribute(HTML.Attribute.BORDER, 0);
		final int hspace = intAttribute(HTML.Attribute.HSPACE, 0);
		final int vspace = intAttribute(HTML.Attribute.VSPACE, 0);
		return new Rectangle(bounds.x + border + hspace, bounds.y + border + vspace,
				Math.max(1, bounds.width - 2 * (border + hspace)),
				Math.max(1, bounds.height - 2 * (border + vspace)));
	}

	private int intAttribute(HTML.Attribute attribute, int defaultValue) {
		final Object value = getElement().getAttributes().getAttribute(attribute);
		if(value == null)
			return defaultValue;
		try {
			return Integer.parseInt(value.toString());
		}
		catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	private float zoom() {
		return HtmlImageViewSettings.imageViewZoom(getDocument(),
				Controller.getCurrentController().getMapViewManager().getZoom());
	}

	static Rectangle scaledBounds(Rectangle bounds, float zoom) {
		if(zoom == 1f)
			return new Rectangle(bounds);
		final int x = (int)Math.floor(bounds.x * zoom);
		final int y = (int)Math.floor(bounds.y * zoom);
		final int maxX = (int)Math.ceil((bounds.x + bounds.width) * zoom);
		final int maxY = (int)Math.ceil((bounds.y + bounds.height) * zoom);
		return new Rectangle(x, y, Math.max(1, maxX - x), Math.max(1, maxY - y));
	}

	private class ScaledImageObserver implements ImageObserver {
		@Override
		public boolean imageUpdate(Image image, int flags, int x, int y, int width, int height) {
			final Container container = getContainer();
			final Rectangle bounds = repaintBounds;
			if(container instanceof Component && bounds != null && shouldRepaint(flags))
				((Component) container).repaint(0, bounds.x, bounds.y, bounds.width, bounds.height);
			return (flags & (ALLBITS | ABORT | ERROR)) == 0;
		}

		private boolean shouldRepaint(int flags) {
			return (flags & (FRAMEBITS | ALLBITS)) != 0;
		}
	}
}
