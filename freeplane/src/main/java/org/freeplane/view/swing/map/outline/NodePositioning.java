package org.freeplane.view.swing.map.outline;

import java.awt.Rectangle;

class NodePositioning {
    private OutlineGeometry geometry;
	private int duplicateItemsHeight;

    NodePositioning(OutlineGeometry geometry, int duplicateItemsHeight) {
        this.geometry = geometry;
        this.duplicateItemsHeight = duplicateItemsHeight;
    }

    void updateGeometry(OutlineGeometry geometry) {
        this.geometry = geometry;
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
