package org.freeplane.features.bookmarks.mindmapmode;

import org.freeplane.features.map.NodeModel;

public final class BookmarkScope {
    private BookmarkScope() {
    }

    public static boolean isAtOrBelow(NodeModel node, NodeModel scopeRoot) {
        return node != null && scopeRoot != null && (node == scopeRoot || node.isDescendantOf(scopeRoot));
    }
}
