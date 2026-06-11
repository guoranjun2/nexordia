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
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.geom.Path2D;

import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.link.CollisionDetector;

/**
 * This class represents a sharp linear Edge of a MindMap.
 */
public class SharpLinearEdgeView extends SharpEdgeView {

	public SharpLinearEdgeView(
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
		final Polygon polygon = createPolygon();
		g.fillPolygon(polygon);
	}

	private Polygon createPolygon() {
		final int deltaX = getDeltaX();
		final int deltaY = getDeltaY();

		if (start != shapeStart) {
			/*
			 * 兼容 shapeStart 与 start 不同的情况。
			 * 例如 folding mark 偏移。
			 */
			final int xs[] = {
					start.x,
					shapeStart.x + deltaX,
					end.x,
					shapeStart.x - deltaX
			};

			final int ys[] = {
					start.y,
					shapeStart.y + deltaY,
					end.y,
					shapeStart.y - deltaY
			};

			return new Polygon(xs, ys, 4);
		}
		else {
			final int xs[] = {
					shapeStart.x + deltaX,
					end.x,
					shapeStart.x - deltaX
			};

			final int ys[] = {
					shapeStart.y + deltaY,
					end.y,
					shapeStart.y - deltaY
			};

			return new Polygon(xs, ys, 3);
		}
	}

	@Override
	public boolean detectCollision(final Point p) {
		/*
		 * 用实际填充多边形做碰撞检测，比原来那种临时构造的三角形更一致。
		 */
		return new CollisionDetector().detectCollision(p, createPolygon());
	}

	@SuppressWarnings("unused")
	private Path2D createCenterLinePath() {
		final Path2D line = new Path2D.Float();

		if (start != shapeStart) {
			line.moveTo(start.x, start.y);
			line.lineTo(shapeStart.x, shapeStart.y);
		}
		else {
			line.moveTo(shapeStart.x, shapeStart.y);
		}

		line.lineTo(end.x, end.y);

		return line;
	}
}
