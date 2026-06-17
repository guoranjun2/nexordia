package org.freeplane.view.swing.map.cloud;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.Path2D;

import org.freeplane.core.util.PaintPerformanceMonitor;
import org.freeplane.features.cloud.CloudModel;
import org.freeplane.view.swing.map.NodeView;

public class ArcCloudView extends CloudView {

	private Path2D.Float path;

	ArcCloudView(CloudModel cloudModel, NodeView source) {
	    super(cloudModel, source);
    }

	@Override
	protected void paintDecoration(Graphics2D g, Graphics2D gstroke) {
		final Polygon p = getCoordinates();
		final int pointCount = p.npoints - 1;
		if (pointCount < 3) {
			fillPolygon(p, g);
			return;
		}
		final Path2D.Float smoothPath = getSmoothPath(p, pointCount);
		final long fillStart = PaintPerformanceMonitor.start();
		g.fill(smoothPath);
		PaintPerformanceMonitor.record(PaintPerformanceMonitor.CLOUD_FILL, fillStart);
		final long drawStart = PaintPerformanceMonitor.start();
		gstroke.draw(smoothPath);
		PaintPerformanceMonitor.record(PaintPerformanceMonitor.CLOUD_DRAW, drawStart);
	}

	private Path2D.Float getSmoothPath(Polygon p, int pointCount) {
		if(path == null) {
			final long start = PaintPerformanceMonitor.start();
			try {
				path = createSmoothPath(p, pointCount);
			}
			finally {
				PaintPerformanceMonitor.record(PaintPerformanceMonitor.CLOUD_PATH, start);
			}
		}
		return path;
	}

	private Path2D.Float createSmoothPath(Polygon p, int pointCount) {
		final int[] xpoints = p.xpoints;
		final int[] ypoints = p.ypoints;
		final Path2D.Float path = new Path2D.Float();
		final double centerX = calculateCenter(xpoints, pointCount);
		final double centerY = calculateCenter(ypoints, pointCount);
		final double distanceToConvexHull = getDistanceToConvexHull() * 2.2;
		path.moveTo(xpoints[0], ypoints[0]);
		for (int i = 0; i < pointCount; i++) {
			final int nextIndex = (i + 1) % pointCount;
			appendArc(path, xpoints[i], ypoints[i], xpoints[nextIndex], ypoints[nextIndex],
					centerX, centerY, distanceToConvexHull);
		}
		path.closePath();
		return path;
	}

	private double calculateCenter(int[] points, int pointCount) {
		double sum = 0;
		for (int i = 0; i < pointCount; i++) {
			sum += points[i];
		}
		return sum / pointCount;
	}

	private void appendArc(Path2D.Float path, double x0, double y0, double x1, double y1,
	                       double centerX, double centerY, double distanceToConvexHull) {
		final double dx = x1 - x0;
		final double dy = y1 - y0;
		final double length = Math.sqrt(dx * dx + dy * dy);
		if (length == 0) {
			return;
		}
		final double middleX = x0 + dx / 2d;
		final double middleY = y0 + dy / 2d;
		double normalX = -dy / length;
		double normalY = dx / length;
		if (normalX * (middleX - centerX) + normalY * (middleY - centerY) < 0) {
			normalX = -normalX;
			normalY = -normalY;
		}
		path.quadTo((float) (middleX + distanceToConvexHull * normalX),
				(float) (middleY + distanceToConvexHull * normalY), (float) x1, (float) y1);
	}

	@Override
	protected void paintDecoration(final Graphics2D g, final Graphics2D gstroke, final double x0, final double y0,
                                 final double x1, final double y1, double dx, double dy, double dxn, double dyn) {
    }
	
	
}
