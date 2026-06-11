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
import java.awt.geom.Path2D;

import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.link.CollisionDetector;

/**
 * This class represents a single straight Edge of a MindMap.
 */
public class LinearEdgeView extends EdgeView {

	public LinearEdgeView(
			NodeView source,
			NodeView target,
			Component paintedComponent,
			boolean highlightsAscendantEdge
	) {
		super(source, target, paintedComponent, highlightsAscendantEdge);
	}


	@Override
	protected boolean usesDynamicConnectorPortsForFreeNode(boolean targetIsFree) {
		return targetIsFree;
	}

	@Override
	protected void draw(final Graphics2D g) {
		final Path2D path = createPath();

		g.draw(path);

		if (drawHiddenParentEdge()) {
			g.setColor(g.getBackground());
			g.setStroke(EdgeView.getEclipsedStroke());
			g.draw(path);
		}
	}

	/**
	 * 创建直线路径。
	 *
	 * 逻辑：
	 *   1. 如果 start 和 shapeStart 不同，先画 start -> shapeStart；
	 *      这是为了兼容原来 folding mark 的偏移。
	 *
	 *   2. 然后画 shapeStart -> end。
	 *
	 * 对 FreeNode 来说，EdgeView 已经根据 dx/dy 重新选好了
	 * TOP/BOTTOM/LEFT/RIGHT 入口，所以这里不需要再判断方向。
	 */
	private Path2D createPath() {
		final Path2D path = new Path2D.Float();

		if (start != shapeStart) {
			path.moveTo(start.x, start.y);
			path.lineTo(shapeStart.x, shapeStart.y);
		}
		else {
			path.moveTo(shapeStart.x, shapeStart.y);
		}

		path.lineTo(end.x, end.y);

		return path;
	}

	@Override
	public boolean detectCollision(final Point p) {
		return new CollisionDetector().detectCollision(p, createPath());
	}
}