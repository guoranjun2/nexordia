package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;

class NodePositioning {
    private OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;

    NodePositioning(OutlineGeometry geometry, VisibleOutlineState visibleState) {
        this.geometry = geometry;
        this.visibleState = visibleState;
    }

    void updateGeometry(OutlineGeometry geometry) {
        this.geometry = geometry;
    }

    int calculateNodeLevel(TreeNode node) {
        return node != null ? node.getLevel() : -1;
    }

    Point calculateNavigationButtonPosition(TreeNode node, boolean isBreadcrumb, int rowIndex, int breadcrumbAreaHeight) {

        final int level = calculateNodeLevel(node);
        final int baseX = geometry.calculateNavigationButtonX(level);
        int y;
        if (isBreadcrumb) {
            y = rowIndex * geometry.rowHeight;
        } else {
            int nodeIndex = visibleState.findNodeIndexInVisibleList(node);
            int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
            int contentAreaIndex = nodeIndex - breadcrumbNodeCount;

            y = breadcrumbAreaHeight + contentAreaIndex * geometry.rowHeight;
        }

		return new Point(baseX, y);
    }

    Point calculateViewportPosition(int startFromNodeIndex, int breadcrumbAreaHeight) {
        int targetY = (startFromNodeIndex * geometry.rowHeight) - breadcrumbAreaHeight;
        targetY = Math.max(0, targetY);
        return new Point(0, targetY);
    }

    Rectangle calculateBlockBounds(int blockIndex, int blockSize, int panelWidth) {
        int start = blockIndex * blockSize;

        int end = Math.min(start + blockSize, visibleState.getVisibleNodeCount());
        int visibleNodesInBlock = end - start;

        int blockY = start * geometry.rowHeight;
        int blockHeight = visibleNodesInBlock * geometry.rowHeight;

        return new Rectangle(0, blockY, panelWidth, blockHeight);
    }

    int calculateFirstVisibleNodeIndex(Rectangle viewRect, int breadcrumbAreaHeight) {
        int effectiveViewportY = viewRect.y + breadcrumbAreaHeight;
        return Math.max(0, (effectiveViewportY + geometry.rowHeight - 1) / geometry.rowHeight);
    }
}
