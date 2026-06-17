package org.freeplane.core.ui.components.html;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;

import org.freeplane.features.mode.Controller;
import org.freeplane.core.util.PaintPerformanceMonitor;

class LazyScaledImageView extends View {
	private static final int DEFAULT_WIDTH = 64;
	private static final int DEFAULT_HEIGHT = 48;
	private static final int VIEWPORT_CACHE_MARGIN_DIVISOR = 2;

	private Dimension naturalSize;
	private boolean naturalSizeKnown;
	private final Runnable repaintCallback = this::repaintHost;
	private URL source;
	private String sourceKey;
	private Rectangle repaintBounds;
	private int width;
	private int height;

	LazyScaledImageView(Element elem) {
		super(elem);
	}

	@Override
	public float getPreferredSpan(int axis) {
		final Dimension preferredSize = preferredSize();
		if(axis == X_AXIS)
			return preferredSize.width + horizontalInsets();
		if(axis == Y_AXIS)
			return preferredSize.height + verticalInsets();
		throw new IllegalArgumentException("Invalid axis: " + axis);
	}

	@Override
	public void paint(Graphics graphics, Shape allocation) {
		final long start = PaintPerformanceMonitor.start();
		try {
		final Rectangle bounds = allocation instanceof Rectangle ? (Rectangle) allocation : allocation.getBounds();
		final Rectangle imageBounds = imageBounds(bounds);
		repaintBounds = scaledBounds(imageBounds, zoom());
		if(! isInVisibleViewport(bounds)) {
			paintPlaceholder(graphics, imageBounds);
			return;
		}
		final URL imageSource = source();
		if(imageSource == null) {
			paintPlaceholder(graphics, imageBounds);
			return;
		}
		final Dimension targetSize = targetSize(graphics, imageBounds);
		final BufferedImage image = HtmlImageCache.INSTANCE.getOrSchedule(imageSource, sourceKey(), targetSize.width,
				targetSize.height, repaintCallback);
		if(image == null) {
			paintPlaceholder(graphics, imageBounds);
			return;
		}
		graphics.drawImage(image, imageBounds.x, imageBounds.y, imageBounds.width, imageBounds.height, null);
		}
		finally {
			PaintPerformanceMonitor.record(PaintPerformanceMonitor.LAZY_IMAGE, start);
		}
	}

	@Override
	public Shape modelToView(int pos, Shape allocation, Position.Bias bias) throws BadLocationException {
		final int startOffset = getStartOffset();
		final int endOffset = getEndOffset();
		if(pos >= startOffset && pos <= endOffset)
			return allocation;
		throw new BadLocationException(String.valueOf(pos), pos);
	}

	@Override
	public int viewToModel(float x, float y, Shape allocation, Position.Bias[] bias) {
		if(bias != null && bias.length > 0)
			bias[0] = Position.Bias.Forward;
		final Rectangle bounds = allocation instanceof Rectangle ? (Rectangle) allocation : allocation.getBounds();
		return x < bounds.getCenterX() ? getStartOffset() : getEndOffset();
	}

	@Override
	public void setSize(float width, float height) {
		this.width = Math.max(0, (int) width);
		this.height = Math.max(0, (int) height);
	}

	@Override
	public void setParent(View parent) {
		if(parent == null)
			HtmlImageCache.INSTANCE.removeRepaintCallback(repaintCallback);
		super.setParent(parent);
	}

	@Override
	public float getAlignment(int axis) {
		if(axis == Y_AXIS)
			return verticalAlignment();
		return super.getAlignment(axis);
	}

	private Dimension preferredSize() {
		final int explicitWidth = intAttribute(HTML.Attribute.WIDTH, -1);
		final int explicitHeight = intAttribute(HTML.Attribute.HEIGHT, -1);
		if(explicitWidth > 0 && explicitHeight > 0)
			return new Dimension(explicitWidth, explicitHeight);
		final Dimension size = naturalSize();
		int preferredWidth = explicitWidth;
		int preferredHeight = explicitHeight;
		if(size != null) {
			if(preferredWidth <= 0)
				preferredWidth = explicitHeight > 0 ? scaledWidth(size, explicitHeight) : size.width;
			if(preferredHeight <= 0)
				preferredHeight = explicitWidth > 0 ? scaledHeight(size, explicitWidth) : size.height;
		}
		if(preferredWidth <= 0)
			preferredWidth = DEFAULT_WIDTH;
		if(preferredHeight <= 0)
			preferredHeight = DEFAULT_HEIGHT;
		return new Dimension(preferredWidth, preferredHeight);
	}

	private int scaledWidth(Dimension size, int preferredHeight) {
		return Math.max(1, (int) Math.round(size.width * (preferredHeight / (double) size.height)));
	}

	private int scaledHeight(Dimension size, int preferredWidth) {
		return Math.max(1, (int) Math.round(size.height * (preferredWidth / (double) size.width)));
	}

	private Dimension naturalSize() {
		if(naturalSizeKnown)
			return new Dimension(naturalSize);
		final URL imageSource = source();
		if(imageSource == null)
			return null;
		final Dimension size = HtmlImageCache.INSTANCE.getImageSizeOrSchedule(imageSource, sourceKey(), repaintCallback);
		if(size == null)
			return null;
		naturalSize = size;
		naturalSizeKnown = true;
		return new Dimension(naturalSize);
	}

	private URL source() {
		if(source != null)
			return source;
		final String src = sourceText();
		if(src == null)
			return null;
		final URL base = ((HTMLDocument)getDocument()).getBase();
		try {
			source = new URL(base, src);
			return source;
		}
		catch (MalformedURLException e) {
			return null;
		}
	}

	private String sourceKey() {
		if(sourceKey == null)
			sourceKey = HtmlImageCache.sourceKey(source());
		return sourceKey;
	}

	private String sourceText() {
		final Object src = getElement().getAttributes().getAttribute(HTML.Attribute.SRC);
		return src != null ? src.toString() : null;
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

	private int horizontalInsets() {
		return 2 * intAttribute(HTML.Attribute.HSPACE, 0) + 2 * intAttribute(HTML.Attribute.BORDER, 0);
	}

	private int verticalInsets() {
		return 2 * intAttribute(HTML.Attribute.VSPACE, 0) + 2 * intAttribute(HTML.Attribute.BORDER, 0);
	}

	private Rectangle imageBounds(Rectangle bounds) {
		final int border = intAttribute(HTML.Attribute.BORDER, 0);
		final int hspace = intAttribute(HTML.Attribute.HSPACE, 0);
		final int vspace = intAttribute(HTML.Attribute.VSPACE, 0);
		return new Rectangle(bounds.x + border + hspace, bounds.y + border + vspace,
				Math.max(1, bounds.width - 2 * (border + hspace)),
				Math.max(1, bounds.height - 2 * (border + vspace)));
	}

	private boolean isInVisibleViewport(Rectangle bounds) {
		final Container container = getContainer();
		if(! (container instanceof Component))
			return true;
		final Component component = (Component) container;
		final JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, component);
		if(viewport == null)
			return true;
		if(! component.isShowing())
			return false;

		final Rectangle visibleRect = viewport.getVisibleRect();
		visibleRect.grow(visibleRect.width / VIEWPORT_CACHE_MARGIN_DIVISOR,
				visibleRect.height / VIEWPORT_CACHE_MARGIN_DIVISOR);

		final Rectangle imageRectangle = SwingUtilities.convertRectangle(component, scaledBounds(bounds, zoom()), viewport);
		return visibleRect.intersects(imageRectangle);
	}

	static Dimension targetSize(Graphics graphics, Rectangle imageBounds) {
		if(! (graphics instanceof Graphics2D))
			return new Dimension(imageBounds.width, imageBounds.height);
		final AffineTransform transform = ((Graphics2D)graphics).getTransform();
		final double scaleX = Math.abs(transform.getScaleX()) + Math.abs(transform.getShearY());
		final double scaleY = Math.abs(transform.getShearX()) + Math.abs(transform.getScaleY());
		return new Dimension(Math.max(1, (int)Math.ceil(imageBounds.width * scaleX)),
				Math.max(1, (int)Math.ceil(imageBounds.height * scaleY)));
	}

	private void repaintHost() {
		final Container container = getContainer();
		if(container instanceof JComponent) {
			final JComponent component = (JComponent) container;
			component.revalidate();
			final Rectangle bounds = repaintBounds;
			if(bounds != null)
				component.repaint(0, bounds.x, bounds.y, bounds.width, bounds.height);
			else
				component.repaint();
		}
	}

	private float zoom() {
		return Controller.getCurrentController().getMapViewManager().getZoom();
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

	private float verticalAlignment() {
		final AttributeSet attributes = getElement().getAttributes();
		final Object alignment = attributes.getAttribute(HTML.Attribute.ALIGN);
		if(alignment == null)
			return 1f;
		final String value = alignment.toString();
		if("top".equals(value))
			return 0f;
		if("middle".equals(value))
			return 0.5f;
		return 1f;
	}

	private void paintPlaceholder(Graphics graphics, Rectangle bounds) {
		final Color oldColor = graphics.getColor();
		graphics.setColor(new Color(0, 0, 0, 30));
		graphics.drawRect(bounds.x, bounds.y, Math.max(0, bounds.width - 1), Math.max(0, bounds.height - 1));
		graphics.setColor(oldColor);
	}
}
