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
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;

import org.freeplane.view.swing.map.MainView.ConnectorLocation;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.link.CollisionDetector;

/**
 * This class represents a sharp bezier Edge of a MindMap.
 */
public class SharpBezierEdgeView extends SharpEdgeView {
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
	 * 不要太大，否则短边会出现明显拖尾。
	 */
	private static final int MIN_HANDLE = 24;

	private Point2D.Float one;
	private Point2D.Float two;

	public SharpBezierEdgeView(
			NodeView source,
			NodeView target,
			Component paintedComponent,
			boolean highlightsAscendantEdge
	) {
		super(source, target, paintedComponent, highlightsAscendantEdge);
	}

	@Override
	public Stroke getStroke() {
		return getStroke(0);
	}

	@Override
	protected boolean usesDynamicConnectorPortsForFreeNode(boolean targetIsFree) {
		return targetIsFree;
	}

	@Override
	protected void draw(final Graphics2D g) {
		final GeneralPath graph = update();
		g.fill(graph);
		g.draw(graph);
	}

	private GeneralPath update() {
		final ConnectorLocation startLocation = getStartConnectorLocation();

		final double dx = end.x - shapeStart.x;
		final double dy = end.y - shapeStart.y;

		/*
		 * 入口/出口方向已经由 EdgeView 统一决定。
		 * 这里只根据入口方向判断是水平曲线还是垂直曲线。
		 */
		final boolean horizontal = isHorizontalConnectorLocation(startLocation);

		final double handle;
		final double sign;

		if (horizontal) {
			handle = calcHandle(Math.abs(dx));
			sign = dx >= 0 ? 1.0 : -1.0;

			one = new Point2D.Float(
					(float) (shapeStart.x + sign * handle),
					shapeStart.y
			);
			two = new Point2D.Float(
					(float) (end.x - sign * handle),
					end.y
			);
		}
		else {
			handle = calcHandle(Math.abs(dy));
			sign = dy >= 0 ? 1.0 : -1.0;

			one = new Point2D.Float(
					shapeStart.x,
					(float) (shapeStart.y + sign * handle)
			);
			two = new Point2D.Float(
					end.x,
					(float) (end.y - sign * handle)
			);
		}

		final float w = (getWidth() / 2f + 1) * getMap().getZoom();
		final float halfW = w / 2f;

		/*
		 * deltaX / deltaY 是 SharpEdgeView 里根据线宽和方向算出的厚度偏移。
		 * 这里沿用原来的填充逻辑。
		 */
		final int deltaX = getDeltaX();
		final int deltaY = getDeltaY();

		/*
		 * 终点附近的厚度偏移。
		 *
		 * 原版使用 endControlPoint 参与计算。
		 * 这里仍然保留这一点，使尖端收束方向和入口/出口方向一致。
		 */
		final Point endControlPoint = getControlPoint(getEndConnectorLocation());

		final float childXctrl;
		final float childYctrl;

		if (horizontal) {
			/*
			 * 水平曲线时，终点厚度主要沿 y 展开。
			 */
			childXctrl = 0;
			childYctrl = endControlPoint.x == 0 ? halfW : Math.signum(endControlPoint.x) * halfW;
		}
		else {
			/*
			 * 垂直曲线时，终点厚度主要沿 x 展开。
			 */
			childXctrl = endControlPoint.y == 0 ? halfW : Math.signum(endControlPoint.y) * halfW;
			childYctrl = 0;
		}

		final GeneralPath graph = new GeneralPath();

		if (start != shapeStart) {
			graph.moveTo(start.x + deltaX, start.y + deltaY);
			graph.lineTo(start.x - deltaX, start.y - deltaY);
			graph.lineTo(shapeStart.x - deltaX, shapeStart.y - deltaY);
		}
		else {
			graph.moveTo(shapeStart.x - deltaX, shapeStart.y - deltaY);
		}

		graph.curveTo(
				one.x - deltaX,
				one.y - deltaY,
				two.x - childXctrl,
				two.y - childYctrl,
				end.x - childXctrl / 4f,
				end.y - childYctrl / 4f
		);

		graph.lineTo(
				end.x + childXctrl / 4f,
				end.y + childYctrl / 4f
		);

		graph.curveTo(
				two.x + childXctrl,
				two.y + childYctrl,
				one.x + deltaX,
				one.y + deltaY,
				shapeStart.x + deltaX,
				shapeStart.y + deltaY
		);

		graph.closePath();

		return graph;
	}

	/**
	 * 按比例计算贝塞尔控制柄。
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
		 * 但不能超过主距离的一半，否则可能回勾。
		 */
		handle = Math.max(handle, Math.min(MIN_HANDLE, mainDistance * 0.5));

		return handle;
	}

	@Override
	public boolean detectCollision(final Point p) {
		/*
		 * one/two 可能还没经过 draw/update 初始化。
		 * 所以这里先 update 一次，保证碰撞检测使用当前曲线。
		 */
		update();

		final Path2D line = new Path2D.Float();

		line.moveTo(start.x, start.y);

		if (start != shapeStart) {
			line.lineTo(shapeStart.x, shapeStart.y);
		}

		line.curveTo(
				one.x,
				one.y,
				two.x,
				two.y,
				end.x,
				end.y
		);

		return new CollisionDetector().detectCollision(p, line);
	}
}