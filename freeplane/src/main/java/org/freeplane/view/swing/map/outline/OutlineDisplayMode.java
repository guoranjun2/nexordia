package org.freeplane.view.swing.map.outline;

import org.freeplane.core.resources.ResourceController;

enum OutlineDisplayMode {
    MAP_VIEW,
    BOOKMARK;
	private static final int BOOKMARK_OUTLINE_LEVEL = 10000;
	static final OutlineDisplayMode DEFAULT = MAP_VIEW;
	int getMinimalOutlineLevel() {
		return this == BOOKMARK ? BOOKMARK_OUTLINE_LEVEL : ResourceController.getResourceController().getIntProperty("minimalFoldableOutlineLevel", 1);
	}
	int getInitialOutlineLevel() {
		return this == BOOKMARK ? BOOKMARK_OUTLINE_LEVEL : ResourceController.getResourceController().getIntProperty("initiallyExpandedOutlineLevel", 1);
	}

	boolean showsNavigationButtons() {
		return this != BOOKMARK;
	}
}
