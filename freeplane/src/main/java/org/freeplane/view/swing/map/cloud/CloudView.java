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
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.LinkedList;
import java.util.Random;
import java.util.Vector;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.cloud.CloudController;
import org.freeplane.features.cloud.CloudModel;
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
	private static final int DEFAULT_CLOUD_PAINTING_MIN_WIDTH = 10;
	private static final int DEFAULT_CLOUD_TEXT_PAINTING_MIN_WIDTH = 50;

	/** the layout functions can get the additional height of the clouded node .
	 * @param cloud */
	static public int getAdditionalHeight(CloudModel cloud, final NodeView source) {
		final CloudView heightCalculator = new CloudViewFactory().createCloudView(cloud, source);
		return (int) (2.2 * heightCalculator.getDistanceToConvexHull());
	}

	protected CloudModel cloudModel;
	protected NodeView source;
	private final int iterativeLevel;
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
		g.dispose();
	}

	public void paintText(final Graphics graphics) {
		final Rectangle paintingBounds = getPaintingBounds();
		if (isCloudTooSmallForPainting(paintingBounds) || isCloudTooSmallForTextPainting(paintingBounds)) {
			return;
		}
		final Graphics2D g = (Graphics2D) graphics.create();
		try {
			paintSourceTextIfSimplified(g, paintingBounds);
		}
		finally {
			g.dispose();
		}
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

	protected Rectangle getPaintingBounds() {
		return getCoordinates().getBounds();
	}

	private void paintSourceTextIfSimplified(Graphics2D g, Rectangle cloudBounds) {
		final MainView mainView = source.getMainView();
		if (mainView == null || !mainView.isPaintingSimplified()) {
			return;
		}
		final String text = getPlainSourceText(mainView);
		if (text.isEmpty()) {
			return;
		}
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

	private String getPlainSourceText(MainView mainView) {
		final String text = mainView.getText();
		if (text == null) {
			return "";
		}
		final String plainText = HtmlUtils.isHtml(text) ? HtmlUtils.htmlToPlain(text) : text;
		final int firstLineEnd = plainText.indexOf('\n');
		return firstLineEnd >= 0 ? plainText.substring(0, firstLineEnd) : plainText;
	}

	protected Polygon getCoordinates() {
        final Polygon p = new Polygon();
        final LinkedList<Point> coordinates = new LinkedList<Point>();
        source.getCoordinates(coordinates);
        if(coordinates.isEmpty())
            return p;
        final ConvexHull hull = new ConvexHull();
        final Vector<Point> res = hull.calculateHull(coordinates);
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
        return p;
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
			for (int j = 0;;) {
				double distanceBetweenPoints = middleDistanceBetweenPoints * random(0.7);
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
