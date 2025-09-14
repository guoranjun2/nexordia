package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JScrollPane;

class OutlineViewport {
    private final JScrollPane scrollPane;
    private final OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;
    private final NodePositioning nodePositioning;

    OutlineViewport(JScrollPane scrollPane, OutlineGeometry geometry, VisibleOutlineState visibleState, NodePositioning nodePositioning) {
        this.scrollPane = scrollPane;
        this.geometry = geometry;
        this.visibleState = visibleState;
        this.nodePositioning = nodePositioning;
    }

    private Rectangle getViewRect() {
        return scrollPane.getViewport().getViewRect();
    }

    private void setViewPosition(Point position) {
        scrollPane.getViewport().setViewPosition(position);
    }

    void setViewPosition(int startFromNodeIndex, int breadcrumbAreaHeight) {
        Point viewPosition = nodePositioning.calculateViewportPosition(startFromNodeIndex, breadcrumbAreaHeight);
        setViewPosition(viewPosition);
    }

	int getViewportHeight() {
		int viewportHeight = scrollPane.getViewport().getHeight();
		return viewportHeight;
	}

    int getViewportWidth() {
        return scrollPane.getViewport().getWidth();
    }

    void refreshViewport() {
        scrollPane.getViewport().revalidate();
        scrollPane.repaint();
    }

    OutlineVisibleBlockRange calculateVisibleBlockRange(int blockSize) {
        Rectangle viewRect = getViewRect();
        int blockHeight = blockSize * geometry.rowHeight;
        int totalBlocks = (visibleState.getVisibleNodeCount() + blockSize - 1) / blockSize;

        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        int adjustedViewY = Math.max(0, viewRect.y - breadcrumbAreaHeight);
        int adjustedViewHeight = viewRect.height;

        int firstBlock = Math.max(0, adjustedViewY / blockHeight);
        int lastBlock = Math.min(totalBlocks - 1, (adjustedViewY + adjustedViewHeight) / blockHeight);

        return new OutlineVisibleBlockRange(firstBlock, lastBlock, breadcrumbAreaHeight);
    }

    int calculateFirstVisibleNodeIndex() {
        Rectangle viewRect = getViewRect();
        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        return nodePositioning.calculateFirstVisibleNodeIndex(viewRect, breadcrumbAreaHeight);
    }


}
