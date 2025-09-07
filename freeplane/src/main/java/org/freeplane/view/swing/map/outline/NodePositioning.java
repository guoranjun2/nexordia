package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;

class NodePositioning {
    private final OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;

    NodePositioning(OutlineGeometry geometry, VisibleOutlineState visibleState) {
        this.geometry = geometry;
        this.visibleState = visibleState;
    }

    int calculateNodeLevel(TreeNode node) {
        return node != null ? node.getLevel() : -1;
    }

    Point calculateNavigationButtonPosition(TreeNode node, boolean isBreadcrumb, int rowIndex, int breadcrumbAreaHeight) {
        int y, level, baseX;

        if (isBreadcrumb) {
            y = rowIndex * geometry.rowHeight;
            level = calculateNodeLevel(node);
            int textButtonX = geometry.calculateTextButtonX(level);
            baseX = Math.max(0, textButtonX - geometry.navButtonsTotalWidth);
        } else {
            int nodeIndex = visibleState.findNodeIndexInVisibleList(node);
            int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
            int contentAreaIndex = nodeIndex - breadcrumbNodeCount;

            y = breadcrumbAreaHeight + contentAreaIndex * geometry.rowHeight;
            level = calculateNodeLevel(node);
            int textButtonX = geometry.calculateTextButtonX(level);
            baseX = Math.max(0, textButtonX - geometry.navButtonsTotalWidth);
        }

        return new Point(baseX, y);
    }

    Point calculateSelectionIconPosition(Rectangle buttonBounds) {
        int iconX = buttonBounds.x + buttonBounds.width;

        int iconY = buttonBounds.y + (buttonBounds.height - geometry.iconDiameter) / 2;
        return new Point(iconX, iconY);
    }

    Point calculateViewportPosition(int startFromNodeIndex, int breadcrumbAreaHeight) {
        int targetY = (startFromNodeIndex * geometry.rowHeight) - breadcrumbAreaHeight;
        targetY = Math.max(0, targetY);
        return new Point(0, targetY);
    }

    Rectangle calculateBlockBounds(int blockIndex, int blockSize, int breadcrumbAreaHeight, int panelWidth) {
        int start = blockIndex * blockSize;
        int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;

        int visibleStart = Math.max(start, breadcrumbNodeCount);
        int end = Math.min(start + blockSize, visibleState.getVisibleNodeCount());
        int visibleNodesInBlock = end - visibleStart;

        int blockY = breadcrumbAreaHeight + (visibleStart - breadcrumbNodeCount) * geometry.rowHeight;
        int blockHeight = visibleNodesInBlock * geometry.rowHeight;

        return new Rectangle(0, blockY, panelWidth, blockHeight);
    }

    int calculateFirstVisibleNodeIndex(Rectangle viewRect, int breadcrumbAreaHeight) {
        int effectiveViewportY = viewRect.y + breadcrumbAreaHeight;
        return Math.max(0, (effectiveViewportY + geometry.rowHeight/2 - 1) / geometry.rowHeight);
    }
}
