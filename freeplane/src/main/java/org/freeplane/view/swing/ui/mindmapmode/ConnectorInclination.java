package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Point;
import java.awt.geom.Rectangle2D;

import javax.swing.SwingUtilities;

import org.freeplane.features.link.ConnectorModel;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

final class ConnectorInclination {
	static final int AUTO = 0;
	static final int HORIZONTAL = 1;
	static final int VERTICAL = 2;
	private static final double HORIZONTAL_MAX_STRENGTH = 1200d;
	private static final double VERTICAL_MAX_STRENGTH = 800d;

	static Inclination forViews(NodeView sourceView, NodeView targetView) {
		return calculate(bounds(sourceView), bounds(targetView), sourceView.isTopOrLeft(), targetView.isTopOrLeft(),
				AUTO);
	}

	static void apply(ConnectorModel connector, NodeView sourceView, NodeView targetView) {
		final Inclination inclination = forViews(sourceView, targetView);
		connector.setStartInclination(inclination.start());
		connector.setEndInclination(inclination.end());
	}

	static Inclination calculate(Rectangle2D source, Rectangle2D target, boolean sourceTopOrLeft,
			boolean targetTopOrLeft, int type) {
		final double sxCenter = source.getCenterX();
		final double syCenter = source.getCenterY();
		final double txCenter = target.getCenterX();
		final double tyCenter = target.getCenterY();
		final double dx = txCenter - sxCenter;
		final double dy = tyCenter - syCenter;
		final double distance = Math.sqrt(dx * dx + dy * dy);
		if (distance < 1d) {
			return new Inclination(new Point(0, 0), new Point(0, 0));
		}

		final double absDx = Math.abs(dx) - (source.getWidth() + target.getWidth()) * 0.5d;
		final double absDy = Math.abs(dy) - (source.getHeight() + target.getHeight()) * 0.5d;
		final boolean horizontal = type == HORIZONTAL || (type == AUTO && absDx > absDy - 50d);
		final double gap = Math.max(0d, horizontal ? absDx : absDy);
		final double centerDistance = Math.abs(horizontal ? dx : dy);
		final double baseStrength = gap * 0.5d;
		final double minLength = Math.min(100d, Math.max(gap, 50d)) * Math.max(1, (int) (gap / 200d));
		final double maxStrength = horizontal ? HORIZONTAL_MAX_STRENGTH : VERTICAL_MAX_STRENGTH;
		double strength;
		if (horizontal) {
			strength = Math.max(minLength, Math.min(baseStrength, maxStrength));
		}
		else {
			final double available = Math.max(0d, gap - 20d);
			final double limit = Math.max(1d, available * 0.5d);
			strength = Math.max(minLength, Math.min(baseStrength, Math.min(maxStrength, limit)));
		}
		strength = Math.min(strength, safeLimit(centerDistance, horizontal));

		int startX = 0;
		int startY = 0;
		int endX = 0;
		int endY = 0;
		if (horizontal) {
			startX = signed(dx, strength);
			endX = signed(-dx, strength);
		}
		else {
			startY = signed(dy, strength);
			endY = signed(-dy, strength);
		}
		if (sourceTopOrLeft) {
			startX = -startX;
		}
		if (targetTopOrLeft) {
			endX = -endX;
		}
		if (startX == 0 && startY == 0) {
			final int fallback = fallback(gap);
			if (horizontal) {
				startX = signed(dx, fallback);
			}
			else {
				startY = signed(dy, fallback);
			}
		}
		if (endX == 0 && endY == 0) {
			final int fallback = fallback(gap);
			if (horizontal) {
				endX = signed(-dx, fallback);
			}
			else {
				endY = signed(-dy, fallback);
			}
		}
		return new Inclination(new Point(startX, startY), new Point(endX, endY));
	}

	private static Rectangle2D bounds(NodeView nodeView) {
		final MapView map = nodeView.getMap();
		final MainView mainView = nodeView.getMainView();
		final Point location = SwingUtilities.convertPoint(mainView, new Point(), map);
		final double zoom = map.getZoom();
		return new Rectangle2D.Double(location.x / zoom, location.y / zoom, mainView.getWidth() / zoom,
				mainView.getHeight() / zoom);
	}

	private static double safeLimit(double centerDistance, boolean horizontal) {
		final double nearThreshold = horizontal ? 400d : 300d;
		if (centerDistance < nearThreshold) {
			return centerDistance * 0.4d;
		}
		final double farRange = horizontal ? 600d : 500d;
		final double ratio = Math.min(1d, (centerDistance - nearThreshold) / farRange);
		return centerDistance * (0.4d + ratio * 0.08d);
	}

	private static int fallback(double gap) {
		return (int) Math.max(20d, 100d * Math.max(1, (int) (gap / 200d)));
	}

	private static int signed(double direction, double value) {
		return (int) Math.round(sign(direction) * value);
	}

	private static double sign(double value) {
		return value < 0d ? -1d : 1d;
	}

	static final class Inclination {
		private final Point start;
		private final Point end;

		private Inclination(Point start, Point end) {
			this.start = start;
			this.end = end;
		}

		Point start() {
			return new Point(start);
		}

		Point end() {
			return new Point(end);
		}
	}

	private ConnectorInclination() {
	}
}
