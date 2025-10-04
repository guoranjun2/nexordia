package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;

class NodePositioning {
    private OutlineGeometry geometry;
    private final VisibleOutlineNodes visibleState;
	private int duplicateItemsHeight;

    NodePositioning(OutlineGeometry geometry, VisibleOutlineNodes visibleState, int duplicateItemsHeight) {
        this.geometry = geometry;
        this.visibleState = visibleState;
        this.duplicateItemsHeight = duplicateItemsHeight;
    }

    void updateGeometry(OutlineGeometry geometry) {
        this.geometry = geometry;
    }

     Point calculateNavigationButtonPosition(TreeNode node, boolean isBreadcrumb, int rowIndex) {

        final int nodeIndex;
        if (isBreadcrumb) {
        	nodeIndex = rowIndex;
        } else {
            nodeIndex = visibleState.findNodeIndexInVisibleList(node);
        }

        final int level = node.getLevel();
        final int baseX = geometry.calculateNavigationButtonX(level);
        final int rowHeight = geometry.rowHeight;
        int y = nodeIndex * rowHeight + 1;
		return new Point(baseX, y);
    }

     int calculateFirstVisibleNodeIndex(Rectangle viewRect, int breadcrumbHeight) {
        int rowHeight = geometry.rowHeight;
        int effectiveViewportY = viewRect.y + breadcrumbHeight - getDuplicateItemsHeight();
        if (effectiveViewportY < 0) {
            effectiveViewportY = 0;
        }
        return Math.max(0, (effectiveViewportY + rowHeight - 1) / rowHeight);
    }

    void setDuplicateItemsHeight(int duplicateItemsHeight) {
        this.duplicateItemsHeight = duplicateItemsHeight;
    }

    int getDuplicateItemsHeight() {
        return duplicateItemsHeight;
    }
}
