package org.freeplane.view.swing.map.cloud;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

class CircleCloudBounds {
	static Ellipse2D.Double fromCenterAndDescendants(Point2D.Double center, List<Rectangle2D.Double> descendants,
													 double padding) {
		if (center == null) {
			return new Ellipse2D.Double();
		}
		double radius = padding;
		for (Rectangle2D.Double descendant : descendants) {
			radius = Math.max(radius, distanceToFarthestCorner(center, descendant) + padding);
		}
		return new Ellipse2D.Double(center.x - radius, center.y - radius, 2 * radius, 2 * radius);
	}

	private static double distanceToFarthestCorner(Point2D.Double center, Rectangle2D.Double descendant) {
		double distance = center.distance(descendant.x, descendant.y);
		distance = Math.max(distance, center.distance(descendant.getMaxX(), descendant.y));
		distance = Math.max(distance, center.distance(descendant.x, descendant.getMaxY()));
		return Math.max(distance, center.distance(descendant.getMaxX(), descendant.getMaxY()));
	}
}
