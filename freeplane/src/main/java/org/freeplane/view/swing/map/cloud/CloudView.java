/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.map.cloud;

import java.awt.BasicStroke;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.net.URL;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.html.HtmlImageRenderer;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.cloud.CloudController;
import org.freeplane.features.cloud.CloudModel;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.map.NodeModel;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

/**
 * This class represents a Cloud around a node.
 */
abstract public class CloudView {
	static final Stroke DEF_STROKE = new BasicStroke(1);
	private static final String CLOUD_PAINTING_MIN_WIDTH = "cloud_painting_min_width";
	private static final String CLOUD_TEXT_PAINTING_MIN_WIDTH = "cloud_text_painting_min_width";
	private static final String PAINT_CLOUD_FOR_INVISIBLE_NODE = "paint_cloud_for_invisible_node";
	private static final String PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE = "paint_cloud_title_for_invisible_node";
	private static final String PAINT_CLOUD_IMAGE = "paint_cloud_image";
	private static final String PAINT_CLOUD_TEXT = "paint_cloud_text";
	private static final String CLOUD_IMAGE_FIXED_ICON = "cloud_image_fixed_icon";
	private static final String CLOUD_IMAGE_OPACITY = "cloud_image_opacity";
	private static final String CLOUD_IMAGE_REPAINT_CALLBACK = "cloud_image_repaint_callback";
	private static final int DEFAULT_CLOUD_PAINTING_MIN_WIDTH = 10;
	private static final int DEFAULT_CLOUD_TEXT_PAINTING_MIN_WIDTH = 10;
	private static final boolean DEFAULT_PAINT_CLOUD_FOR_INVISIBLE_NODE = true;
	private static final boolean DEFAULT_PAINT_CLOUD_IMAGE = true;
	private static final boolean DEFAULT_PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE = false;
	private static final boolean DEFAULT_PAINT_CLOUD_TEXT = true;
	private static final String DEFAULT_CLOUD_IMAGE_FIXED_ICON = "info";
	private static final double DEFAULT_CLOUD_IMAGE_OPACITY = 1d;
	private static final double MINIMUM_CLOUD_IMAGE_OPACITY = 0.05d;
	private static final double MAXIMUM_CLOUD_IMAGE_OPACITY = 1d;
	private static final double MINIMUM_DISTANCE_BETWEEN_CLOUD_POINTS = 1d;

	/** the layout functions can get the additional height of the clouded node .
	 * @param cloud */
	static public int getAdditionalHeight(CloudModel cloud, final NodeView source) {
		final CloudView heightCalculator = new CloudViewFactory().createCloudView(cloud, source);
		return (int) (2.2 * heightCalculator.getDistanceToConvexHull());
	}

	protected CloudModel cloudModel;
	protected NodeView source;
	private final int iterativeLevel;
	private Polygon coordinates;
	private Random random;

	CloudView(final CloudModel cloudModel, final NodeView source) {
		this.cloudModel = cloudModel;
		this.source = source;
		iterativeLevel = getCloudIterativeLevel();
	}

	private int getCloudIterativeLevel() {
		int iterativeLevel = 0;
		for (NodeView parentNode = source.getParentView(); parentNode != null; parentNode = parentNode.getParentView()) {
			if (null != parentNode.getCloudModel()) {
				iterativeLevel++;
			}
		}
		return iterativeLevel;
    }

	public Color getColor() {
		return source.getCloudColor();
	}

	protected double getDistanceToConvexHull() {
		return 20 / (getIterativeLevel() + 1) * getZoom();
	}

	public Color getExteriorColor(final Color color) {
		return color.darker();
	}

	/**
	 * getIterativeLevel() describes the n-th nested cloud that is to be
	 * painted.
	 */
	protected int getIterativeLevel() {
		return iterativeLevel;
	}

	protected MapView getMap() {
		return source.getMap();
	}

	protected CloudModel getModel() {
		return cloudModel;
	}

	/**
	 * Get the width in pixels rather than in width constant (like -1)
	 */
	public int getRealWidth() {
		final int width = getWidth();
		return (width < 1) ? 1 : width;
	}

	public Stroke getStroke() {
		final int width = getWidth();
		if (width < 1) {
			return CloudView.DEF_STROKE;
		}
		return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
	}

	public int getWidth() {
		final NodeModel node = source.getNode();
		return CloudController.getController(source.getMap().getModeController()).getWidth(node);
	}

	protected double getZoom() {
		return getMap().getZoom();
	}

	public void paint(final Graphics graphics) {
		final MainView mainView = source.getMainView();
		if(!shouldPaintCloud(mainView)) {
			return;
		}
		final Rectangle paintingBounds = getPaintingBounds();
		if (isCloudTooSmallForPainting(paintingBounds)) {
			return;
		}
		random = new Random(0);
		final Graphics2D g = (Graphics2D) graphics.create();
		final Graphics2D gstroke = (Graphics2D) g.create();
		final Color color = getColor();
		g.setColor(color);
		/* set a bigger stroke to prevent not filled areas. */
		g.setStroke(getStroke());
		/* now bold */
		gstroke.setColor(getExteriorColor(color));
		gstroke.setStroke(getStroke());
		/*
		 * calculate the distances between two points on the convex hull
		 * depending on the getIterativeLevel().
			 */
			/** get coordinates */
			paintDecoration(g, gstroke);
			paintSourceImageText(g);
			g.dispose();
		}

	public void paintText(final Graphics graphics) {
		final MainView mainView = source.getMainView();
		if (!shouldPaintSourceText(mainView)) {
			return;
		}
		final Rectangle paintingBounds = getPaintingBounds();
		if (isCloudTooSmallForPainting(paintingBounds) || isCloudTooSmallForTextPainting(paintingBounds)) {
			return;
		}
		final Graphics2D g = (Graphics2D) graphics.create();
		try {
			paintSourceText(g, mainView, paintingBounds);
		}
		finally {
			g.dispose();
		}
	}

	private boolean shouldPaintSourceText(MainView mainView) {
		return isCloudTextPaintingEnabled()
				&& mainView != null
				&& ((isSourceVisibleForTextPainting(mainView) && mainView.isPaintingSimplified())
						|| isFixedImageCloud(mainView));
	}

	private boolean shouldPaintSourceImage(MainView mainView) {
		return mainView != null
				&& CloudImagePainting.shouldPaint(isCloudImagePaintingEnabled(), isSourceVisibleForTextPainting(mainView),
						mainView.isPaintingSimplified(), isFixedImageCloud(mainView));
	}

	private boolean isFixedImageCloud(MainView mainView) {
		return CloudImagePainting.shouldKeepTextWithFixedImage(isCloudImagePaintingEnabled(), hasCloudImageFixedIcon(),
				getCloudTitle(mainView).isImage());
	}

	private boolean shouldPaintCloud(MainView mainView) {
		return isSourceVisibleForPainting(mainView) || mainView != null && isFixedImageCloud(mainView);
	}

	private boolean isSourceVisibleForPainting(MainView mainView) {
		return mainView != null
				&& (isCloudPaintingForInvisibleNodeEnabled() || mainView.hasVisiblePaintAnchor(source.getMap()));
	}

	private boolean isSourceVisibleForTextPainting(MainView mainView) {
		return mainView != null
				&& (isCloudTitlePaintingForInvisibleNodeEnabled() || mainView.hasVisiblePaintAnchor(source.getMap()));
	}

	static boolean isCloudTooSmallForPainting(Rectangle bounds) {
		return bounds.width < getCloudPaintingMinWidth();
	}

	private static boolean isCloudTooSmallForTextPainting(Rectangle bounds) {
		return bounds.width < getCloudTextPaintingMinWidth();
	}

	private static int getCloudPaintingMinWidth() {
		return ResourceController.getResourceController().getIntProperty(CLOUD_PAINTING_MIN_WIDTH,
				DEFAULT_CLOUD_PAINTING_MIN_WIDTH);
	}

	private static int getCloudTextPaintingMinWidth() {
		return ResourceController.getResourceController().getIntProperty(CLOUD_TEXT_PAINTING_MIN_WIDTH,
				DEFAULT_CLOUD_TEXT_PAINTING_MIN_WIDTH);
	}

	private static boolean isCloudPaintingForInvisibleNodeEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_FOR_INVISIBLE_NODE,
				DEFAULT_PAINT_CLOUD_FOR_INVISIBLE_NODE);
	}

	private static boolean isCloudTitlePaintingForInvisibleNodeEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE,
				DEFAULT_PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE);
	}

	private static boolean isCloudTextPaintingEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_TEXT,
				DEFAULT_PAINT_CLOUD_TEXT);
	}

	private static boolean isCloudImagePaintingEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_IMAGE,
				DEFAULT_PAINT_CLOUD_IMAGE);
	}

	private static String getCloudImageFixedIcon() {
		return ResourceController.getResourceController().getProperty(CLOUD_IMAGE_FIXED_ICON,
				DEFAULT_CLOUD_IMAGE_FIXED_ICON);
	}

	private static double getCloudImageOpacity() {
		final double opacity = ResourceController.getResourceController().getDoubleProperty(CLOUD_IMAGE_OPACITY,
				DEFAULT_CLOUD_IMAGE_OPACITY);
		return CloudImagePainting.opacityInRange(opacity, MINIMUM_CLOUD_IMAGE_OPACITY, MAXIMUM_CLOUD_IMAGE_OPACITY);
	}

	protected Rectangle getPaintingBounds() {
		return getCoordinates().getBounds();
	}

	private void paintSourceText(Graphics2D g, MainView mainView, Rectangle cloudBounds) {
		final CloudTitle cloudTitle = getCloudTitle(mainView);
		if (cloudTitle.getText().isEmpty()) {
			return;
		}
		paintPlainSourceText(g, mainView, cloudBounds, cloudTitle.getText());
	}

	private void paintSourceImageText(Graphics2D g) {
		final MainView mainView = source.getMainView();
		if (!shouldPaintSourceImage(mainView)) {
			return;
		}
		final Rectangle paintingBounds = getPaintingBounds();
		if (isCloudTooSmallForPainting(paintingBounds) || isCloudTooSmallForTextPainting(paintingBounds)) {
			return;
		}
		final CloudTitle cloudTitle = getCloudTitle(mainView);
		if (!cloudTitle.isImage()) {
			return;
		}
		paintImageSourceText(g, paintingBounds, cloudTitle.getImageSource());
	}

	private boolean hasCloudImageFixedIcon() {
		final String cloudImageFixedIcon = getCloudImageFixedIcon();
		for (NamedIcon icon : source.getNode().getIcons()) {
			if (cloudImageFixedIcon.equals(icon.getName())) {
				return true;
			}
		}
		return false;
	}

	private void paintImageSourceText(Graphics2D g, Rectangle cloudBounds, String imageSource) {
		final URL base = source.getMap().getMap().getURL();
		final Shape clip = g.getClip();
		final Composite composite = g.getComposite();
		try {
			g.clip(cloudBounds);
			setCloudImageOpacity(g);
			HtmlImageRenderer.paintContained(g, cloudBounds, base, imageSource, cloudImageRepaintCallback());
		}
		finally {
			g.setComposite(composite);
			g.setClip(clip);
		}
	}

	private void setCloudImageOpacity(Graphics2D g) {
		final double opacity = getCloudImageOpacity();
		if (opacity < MAXIMUM_CLOUD_IMAGE_OPACITY) {
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) opacity));
		}
	}

	private Runnable cloudImageRepaintCallback() {
		final MapView map = source.getMap();
		final Object callback = map.getClientProperty(CLOUD_IMAGE_REPAINT_CALLBACK);
		if (callback instanceof Runnable) {
			return (Runnable) callback;
		}
		final Runnable repaintCallback = map::repaint;
		map.putClientProperty(CLOUD_IMAGE_REPAINT_CALLBACK, repaintCallback);
		return repaintCallback;
	}

	private void paintPlainSourceText(Graphics2D g, MainView mainView, Rectangle cloudBounds, String text) {
		final Font font = fitFontToBounds(mainView.getFont(), text, g, cloudBounds);
		if (font == null) {
			return;
		}
		g.setFont(font);
		g.setColor(mainView.getForeground());
		final FontMetrics metrics = g.getFontMetrics(font);
		if (metrics.getHeight() > cloudBounds.height) {
			return;
		}
		final String fittedText = fitTextToWidth(text, metrics, cloudBounds.width);
		if (fittedText.isEmpty()) {
			return;
		}
		final int x = cloudBounds.x + (cloudBounds.width - metrics.stringWidth(fittedText)) / 2;
		final int y = cloudBounds.y + (cloudBounds.height - metrics.getHeight()) / 2 + metrics.getAscent();
		final Shape clip = g.getClip();
		try {
			g.clip(cloudBounds);
			g.drawString(fittedText, x, y);
		}
		finally {
			g.setClip(clip);
		}
	}

	private Font fitFontToBounds(Font baseFont, String text, Graphics2D g, Rectangle cloudBounds) {
		final Font smallestFont = baseFont.deriveFont(1f);
		if (doesTextFit(text, g.getFontMetrics(smallestFont), cloudBounds)) {
			float minSize = 1f;
			float maxSize = Math.max(baseFont.getSize2D(), cloudBounds.height);
			for (int i = 0; i < 12; i++) {
				final float middleSize = (minSize + maxSize) / 2f;
				final Font middleFont = baseFont.deriveFont(middleSize);
				if (doesTextFit(text, g.getFontMetrics(middleFont), cloudBounds)) {
					minSize = middleSize;
				}
				else {
					maxSize = middleSize;
				}
			}
			return baseFont.deriveFont(minSize);
		}
		return g.getFontMetrics(smallestFont).getHeight() <= cloudBounds.height ? smallestFont : null;
	}

	private boolean doesTextFit(String text, FontMetrics metrics, Rectangle cloudBounds) {
		return metrics.stringWidth(text) <= cloudBounds.width && metrics.getHeight() <= cloudBounds.height;
	}

	private String fitTextToWidth(String text, FontMetrics metrics, int width) {
		if (metrics.stringWidth(text) <= width) {
			return text;
		}
		final String ellipsis = "...";
		final int ellipsisWidth = metrics.stringWidth(ellipsis);
		if (ellipsisWidth > width) {
			return "";
		}
		int begin = 0;
		int end = text.length();
		while (begin < end) {
			final int middle = (begin + end + 1) / 2;
			if (metrics.stringWidth(text.substring(0, middle)) + ellipsisWidth <= width) {
				begin = middle;
			}
			else {
				end = middle - 1;
			}
		}
		return begin == 0 ? ellipsis : text.substring(0, begin) + ellipsis;
	}

	private CloudTitle getCloudTitle(MainView mainView) {
		final NodeAttributeTableModel attributes = NodeAttributeTableModel.getModel(source.getNode());
		return CloudTitle.from(attributes, mainView.getText());
	}

	protected Polygon getCoordinates() {
		if (coordinates != null) {
			return coordinates;
		}
        final Polygon p = new Polygon();
        final LinkedList<Point> sourceCoordinates = new LinkedList<Point>();
        source.getCoordinates(sourceCoordinates);
        if(sourceCoordinates.isEmpty())
            return this.coordinates = p;
        final ConvexHull hull = new ConvexHull();
        final Vector<Point> res = hull.calculateHull(sourceCoordinates);
        Point lastPt = null;
        for (int i = 0; i < res.size(); ++i) {
            final Point pt = res.get(i);
            if(!pt.equals(lastPt)){
                p.addPoint(pt.x, pt.y);
                lastPt = pt;
            }
        }
        final Point pt = res.get(0);
        p.addPoint(pt.x, pt.y);
        return this.coordinates = p;
	}

	protected void paintDecoration(Graphics2D g, Graphics2D gstroke){
	    Polygon p = getCoordinates();
		fillPolygon(p, g);
		double middleDistanceBetweenPoints = calcDistanceBetweenPoints();
		final int[] xpoints = p.xpoints;
		final int[] ypoints = p.ypoints;
		final Point lastPoint = new Point(xpoints[0], ypoints[0]);
		double x0, y0;
		x0 = lastPoint.x;
		y0 = lastPoint.y;
		/* close the path: */
		double x2, y2; /* the drawing start points. */
		x2 = x0;
		y2 = y0;
		for (int i = p.npoints - 2; i >= 0; --i) {
			final Point nextPoint = new Point(xpoints[i], ypoints[i]);
			double x1, y1, x3, y3, dx, dy, dxn, dyn;
			x1 = nextPoint.x;
			y1 = nextPoint.y;
			dx = x1 - x0; /* direction of p0 -> p1 */
			dy = y1 - y0;
			final double length = Math.sqrt(dx * dx + dy * dy);
			dxn = dx / length; /* normalized direction of p0 -> p1 */
			dyn = dy / length;
			for (double j = 0;;) {
				double distanceBetweenPoints = Math.max(MINIMUM_DISTANCE_BETWEEN_CLOUD_POINTS,
						middleDistanceBetweenPoints * random(0.7));
				if (j + 2* distanceBetweenPoints < length) {
					j += distanceBetweenPoints;
					x3 = x0 + j * dxn;
					/* the drawing end point.*/
					y3 = y0 + j * dyn;
				}
				else {
					/* last point */
					break;
				}
				paintDecoration(g, gstroke, x2, y2, x3, y3);
				x2 = x3;
				y2 = y3;
			}

			paintDecoration(g, gstroke, x2, y2, x1, y1);
			x2 = x1;
			y2 = y1;
			x0 = x1;
			y0 = y1;
		}
	}

	protected void fillPolygon(final Polygon p, Graphics2D g) {
	    g.fillPolygon(p);
		g.drawPolygon(p);
    }

	protected void paintDecoration(Graphics2D g, Graphics2D gstroke, double x0, double y0, double x1, double y1) {
			double dx, dy;
			dx = x1 - x0;
			dy = y1 - y0;
			final double length = Math.sqrt(dx * dx + dy * dy);
			double dxn, dyn;
			dxn = dx / length;
			dyn = dy / length;
			paintDecoration(g, gstroke, x0, y0, x1, y1, dx, dy, dxn, dyn);
		}

	abstract protected void paintDecoration(Graphics2D g, Graphics2D gstroke, double x0, double y0, double x1, double y1,
                                 double dx, double dy, double dxn, double dyn);

    protected double calcDistanceBetweenPoints() {
	    final double distanceBetweenPoints = getDistanceToConvexHull();
		return distanceBetweenPoints;
    }

	protected double random(double min) {
	    return (min + (1-min) * random.nextDouble());
    }
}
