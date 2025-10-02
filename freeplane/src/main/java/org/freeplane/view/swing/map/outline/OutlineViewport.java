package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JScrollPane;
import javax.swing.JComponent;

class OutlineViewport {
    private final JScrollPane scrollPane;
    private final VisibleOutlineNodes visibleState;
    private final NodePositioning nodePositioning;

    OutlineViewport(JScrollPane scrollPane, VisibleOutlineNodes visibleState, NodePositioning nodePositioning) {
        this.scrollPane = scrollPane;
        this.visibleState = visibleState;
        this.nodePositioning = nodePositioning;
    }

    private Rectangle getViewRect() {
        return scrollPane.getViewport().getViewRect();
    }

    private void setViewPosition(Point position) {
        scrollPane.getViewport().setViewPosition(position);
    }

    void setViewPosition(int startFromNodeIndex, int breadcrumbHeight) {
        Point viewPosition = nodePositioning.calculateViewportPosition(startFromNodeIndex, breadcrumbHeight);
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
        java.awt.Component view = scrollPane.getViewport().getView();
        if (view instanceof JComponent) {
            ((JComponent) view).revalidate();
        }
        scrollPane.getViewport().revalidate();
        scrollPane.repaint();
    }

    OutlineVisibleBlockRange calculateVisibleBlockRange(int blockSize) {
        Rectangle viewRect = getViewRect();
        int blockHeight = blockSize * OutlineGeometry.getInstance().rowHeight;
        int totalBlocks = (visibleState.getVisibleNodeCount() + blockSize - 1) / blockSize;

        int breadcrumbHeight = visibleState.getBreadcrumbHeight();
        int contentOffset = nodePositioning.getContentAreaOffset();
        int adjustedViewY = Math.max(0, viewRect.y - breadcrumbHeight - contentOffset);
        int adjustedViewHeight = viewRect.height;

        int firstBlock = Math.max(0, adjustedViewY / blockHeight);
        int lastBlock = Math.min(totalBlocks - 1, (adjustedViewY + adjustedViewHeight) / blockHeight);

        return new OutlineVisibleBlockRange(firstBlock, lastBlock, breadcrumbHeight);
    }

    int calculateFirstVisibleNodeIndex() {
        Rectangle viewRect = getViewRect();
        int breadcrumbHeight = visibleState.getBreadcrumbHeight();
        return nodePositioning.calculateFirstVisibleNodeIndex(viewRect, breadcrumbHeight);
    }


}
