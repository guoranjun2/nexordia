package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;

class NodePositioning {
    private OutlineGeometry geometry;
    private final VisibleOutlineNodes visibleState;
    private BreadcrumbMode breadcrumbMode;

    NodePositioning(OutlineGeometry geometry, VisibleOutlineNodes visibleState, BreadcrumbMode breadcrumbMode) {
        this.geometry = geometry;
        this.visibleState = visibleState;
        this.breadcrumbMode = breadcrumbMode;
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
            int rowHeight = geometry.rowHeight;
            int breadcrumbNodeCount = breadcrumbAreaHeight / rowHeight;
            int contentAreaIndex = Math.max(0, nodeIndex - breadcrumbNodeCount + reservedBreadcrumbNodeCount());

            y = breadcrumbAreaHeight + contentAreaIndex * rowHeight;
        }

		return new Point(baseX, y);
    }

    Point calculateViewportPosition(int startFromNodeIndex, int breadcrumbAreaHeight) {
        int rowHeight = geometry.rowHeight;
        int targetY = (startFromNodeIndex * rowHeight) - breadcrumbAreaHeight;
        targetY = Math.max(0, targetY);
        return new Point(0, targetY);
    }

    Rectangle calculateBlockBounds(int blockIndex, int blockSize, int panelWidth) {
        int start = blockIndex * blockSize;

        int end = Math.min(start + blockSize, visibleState.getVisibleNodeCount());
        int visibleNodesInBlock = end - start;

        int rowHeight = geometry.rowHeight;
        int blockY = (start + reservedBreadcrumbNodeCount()) * rowHeight;
        int blockHeight = visibleNodesInBlock * rowHeight;

        return new Rectangle(0, blockY, panelWidth, blockHeight);
    }

    int calculateFirstVisibleNodeIndex(Rectangle viewRect, int breadcrumbAreaHeight) {
        int rowHeight = geometry.rowHeight;
        int effectiveViewportY = viewRect.y + breadcrumbAreaHeight;
        return Math.max(0, (effectiveViewportY + rowHeight - 1) / rowHeight);
    }

	private int reservedBreadcrumbNodeCount() {
		return isSelectionDrivenBreadcrumbMode()? 1: 0;
	}

    void setBreadcrumbMode(BreadcrumbMode breadcrumbMode) {
        this.breadcrumbMode = breadcrumbMode;
    }

    private boolean isSelectionDrivenBreadcrumbMode() {
        return breadcrumbMode == BreadcrumbMode.FOLLOW_SELECTED_ITEM;
    }
}
