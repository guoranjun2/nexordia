package org.freeplane.view.swing.map.cloud;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.freeplane.features.cloud.CloudModel;
import org.freeplane.view.swing.map.NodeView;

public class CircleCloudView extends CloudView {
	CircleCloudView(CloudModel cloudModel, NodeView source) {
		super(cloudModel, source);
	}

	@Override
	protected void fillPolygon(Polygon p, Graphics2D g) {
	}

	@Override
	protected void paintDecoration(Graphics2D g, Graphics2D gstroke) {
		final Ellipse2D.Double circle = getCircle();
		g.fill(circle);
		gstroke.draw(circle);
	}

	@Override
	protected void paintDecoration(Graphics2D g, Graphics2D gstroke, double x0, double y0, double x1, double y1,
								   double dx, double dy, double dxn, double dyn) {
	}

	@Override
	protected double getDistanceToConvexHull() {
		return 0.5 * super.getDistanceToConvexHull();
	}

	private Ellipse2D.Double getCircle() {
		final CenterAndDescendants centerAndDescendants = new CenterAndDescendants();
		addNodeBounds(source, 0, 0, true, centerAndDescendants);
		return CircleCloudBounds.fromCenterAndDescendants(centerAndDescendants.center,
				centerAndDescendants.descendants, getDistanceToConvexHull());
	}

	private void addNodeBounds(NodeView nodeView, int x, int y, boolean isCenterNode, CenterAndDescendants result) {
		if (!nodeView.isVisible()) {
			return;
		}
		if (nodeView.isContentVisible()) {
			final JComponent content = nodeView.getContent();
			final Rectangle2D.Double bounds = new Rectangle2D.Double(x + content.getX(), y + content.getY(),
					content.getWidth(), content.getHeight());
			final Point2D.Double center = new Point2D.Double(x + content.getX() + content.getWidth() / 2.0,
					y + content.getY() + content.getHeight() / 2.0);
			if (isCenterNode) {
				result.center = center;
			}
			else {
				result.descendants.add(bounds);
			}
		}
		for (NodeView child : nodeView.getChildrenViews()) {
			addNodeBounds(child, x + child.getX(), y + child.getY(), false, result);
		}
	}

	private static class CenterAndDescendants {
		Point2D.Double center;
		final List<Rectangle2D.Double> descendants = new ArrayList<Rectangle2D.Double>();
	}
}
