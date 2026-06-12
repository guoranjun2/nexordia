package org.freeplane.view.swing.map.cloud;

import java.awt.Rectangle;
import java.util.List;

class RectangleCloudBounds {
	static Rectangle fromContents(List<Rectangle> contents, int padding, boolean expandHorizontally,
								  boolean expandVertically) {
		final Rectangle bounds = union(contents);
		if (bounds.isEmpty()) {
			return bounds;
		}
		if (expandHorizontally) {
			bounds.x -= padding;
			bounds.width += 2 * padding;
		}
		if (expandVertically) {
			bounds.y -= padding;
			bounds.height += 2 * padding;
		}
		return bounds;
	}

	private static Rectangle union(List<Rectangle> contents) {
		final Rectangle bounds = new Rectangle();
		boolean hasContents = false;
		for (Rectangle content : contents) {
			if (!hasContents) {
				bounds.setBounds(content);
				hasContents = true;
			}
			else {
				bounds.add(content);
			}
		}
		return bounds;
	}
}
