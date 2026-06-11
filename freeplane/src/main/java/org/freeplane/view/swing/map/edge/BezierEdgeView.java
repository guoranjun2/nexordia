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
package org.freeplane.view.swing.map.edge;

import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.GeneralPath;

import org.freeplane.view.swing.map.MainView.ConnectorLocation;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.link.CollisionDetector;

/**
 * This class represents a single Edge of a MindMap.
 */
public class BezierEdgeView extends EdgeView {
	/*
	 * 控制柄比例。
	 *
	 * 0.25：轻
	 * 0.30：自然
	 * 0.32：默认
	 * 0.35：舒展
	 * 0.40：圆润
	 */
	private static final double HANDLE_RATIO = 0.32;

	/*
	 * 近距离兜底。
	 * 不要太大，否则短边会拖出一截。
	 */
	private static final int MIN_HANDLE = 24;

	public BezierEdgeView(
			NodeView source,
			NodeView target,
			Component paintedComponent,
			boolean highlightsAscendantEdge
	) {
		super(source, target, paintedComponent, highlightsAscendantEdge);
	}

	@Override
	protected void draw(final Graphics2D g) {
		final GeneralPath graph = update();
		g.draw(graph);

		if (drawHiddenParentEdge()) {
			g.setColor(g.getBackground());
			g.setStroke(EdgeView.getEclipsedStroke());
			g.draw(graph);
		}
	}

	@Override
	protected boolean usesDynamicConnectorPortsForFreeNode(boolean targetIsFree) {
		return targetIsFree;
	}

	private GeneralPath update() {
		final ConnectorLocation startLocation = getStartConnectorLocation();

		final double dx = end.x - shapeStart.x;
		final double dy = end.y - shapeStart.y;

		/*
		 * 入口/出口方向已经由 EdgeView 决定。
		 * BezierEdgeView 只根据当前入口方向画曲线。
		 */
		final boolean horizontal = isHorizontalConnectorLocation(startLocation);

		final double c1x;
		final double c1y;
		final double c2x;
		final double c2y;

		if (horizontal) {
			final double handle = calcHandle(Math.abs(dx));
			final double sign = dx >= 0 ? 1.0 : -1.0;

			c1x = shapeStart.x + sign * handle;
			c1y = shapeStart.y;

			c2x = end.x - sign * handle;
			c2y = end.y;
		}
		else {
			final double handle = calcHandle(Math.abs(dy));
			final double sign = dy >= 0 ? 1.0 : -1.0;

			c1x = shapeStart.x;
			c1y = shapeStart.y + sign * handle;

			c2x = end.x;
			c2y = end.y - sign * handle;
		}

		final GeneralPath graph = new GeneralPath();

		if (start != shapeStart) {
			graph.moveTo(start.x, start.y);
			graph.lineTo(shapeStart.x, shapeStart.y);
		}
		else {
			graph.moveTo(shapeStart.x, shapeStart.y);
		}

		graph.curveTo(
				c1x, c1y,
				c2x, c2y,
				end.x, end.y
		);

		return graph;
	}

	/**
	 * 按比例计算控制柄。
	 *
	 * 注意：
	 * start / shapeStart / end 已经是 paintedComponent 坐标，
	 * 这里不要再 getMap().getZoomed(...)。
	 */
	private double calcHandle(final double mainDistance) {
		if (mainDistance <= 1) {
			return 0;
		}

		double handle = mainDistance * HANDLE_RATIO;

		/*
		 * 近距离给一点最小弧度；
		 * 但不能超过主距离的一半，否则会回勾。
		 */
		handle = Math.max(handle, Math.min(MIN_HANDLE, mainDistance * 0.5));

		return handle;
	}

	@Override
	public boolean detectCollision(final Point p) {
		final GeneralPath graph = update();
		return new CollisionDetector().detectCollision(p, graph);
	}
}