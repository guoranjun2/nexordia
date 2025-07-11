package org.freeplane.view.swing.map.outline;

import java.awt.Rectangle;
import java.awt.Point;
import java.util.List;

import javax.swing.JScrollPane;

class OutlineViewport {
    private final JScrollPane scrollPane;
    private final OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;
    private final NodePositioning nodePositioning;
    
    public OutlineViewport(JScrollPane scrollPane, OutlineGeometry geometry, VisibleOutlineState visibleState, NodePositioning nodePositioning) {
        this.scrollPane = scrollPane;
        this.geometry = geometry;
        this.visibleState = visibleState;
        this.nodePositioning = nodePositioning;
    }
    
    public Rectangle getViewRect() {
        return scrollPane.getViewport().getViewRect();
    }
    
    public void setViewPosition(Point position) {
        scrollPane.getViewport().setViewPosition(position);
    }
    
    public void setViewPosition(int startFromNodeIndex, int breadcrumbAreaHeight) {
        Point viewPosition = nodePositioning.calculateViewportPosition(startFromNodeIndex, breadcrumbAreaHeight);
        setViewPosition(viewPosition);
    }
    
    public void scrollToNode(TreeNode node) {
        List<FlatNode> visibleNodes = visibleState.getVisibleNodes();
        int nodeIndex = findNodeIndex(node, visibleNodes);
        if (nodeIndex >= 0) {
            int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
            int y = breadcrumbAreaHeight + nodeIndex * geometry.rowHeight;
            Rectangle targetRect = new Rectangle(0, y, getViewRect().width, geometry.rowHeight);
            scrollToRect(targetRect);
        }
    }
    
    private void scrollToRect(Rectangle targetRect) {
        if (scrollPane.getParent() instanceof javax.swing.JComponent) {
            ((javax.swing.JComponent) scrollPane.getParent()).scrollRectToVisible(targetRect);
        }
    }
    
    public int getPageSize() {
        int viewportHeight = scrollPane.getViewport().getHeight();
        return Math.max(1, viewportHeight / geometry.rowHeight);
    }
    
    public int getViewportWidth() {
        return scrollPane.getViewport().getWidth();
    }
    
    public int getViewportHeight() {
        return scrollPane.getViewport().getHeight();
    }
    
    public void refreshViewport() {
        scrollPane.getViewport().revalidate();
        scrollPane.repaint();
    }
    
    public VisibleBlockRange calculateVisibleBlockRange(int blockSize) {
        Rectangle viewRect = getViewRect();
        int blockHeight = blockSize * geometry.rowHeight;
        List<FlatNode> visibleNodes = visibleState.getVisibleNodes();
        int totalBlocks = (visibleNodes.size() + blockSize - 1) / blockSize;

        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        int adjustedViewY = Math.max(0, viewRect.y - breadcrumbAreaHeight);
        int adjustedViewHeight = viewRect.height;

        int firstBlock = Math.max(0, adjustedViewY / blockHeight);
        int lastBlock = Math.min(totalBlocks - 1, (adjustedViewY + adjustedViewHeight) / blockHeight);
        
        return new VisibleBlockRange(firstBlock, lastBlock, breadcrumbAreaHeight);
    }
    
    public int calculateFirstVisibleNodeIndex() {
        Rectangle viewRect = getViewRect();
        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        return nodePositioning.calculateFirstVisibleNodeIndex(viewRect, breadcrumbAreaHeight);
    }
    
    private int findNodeIndex(TreeNode node, List<FlatNode> visibleNodes) {
        for (int i = 0; i < visibleNodes.size(); i++) {
            if (visibleNodes.get(i).node.id.equals(node.id)) {
                return i;
            }
        }
        return -1;
    }
    
    public static class VisibleBlockRange {
        public final int firstBlock;
        public final int lastBlock;
        public final int breadcrumbAreaHeight;
        
        private VisibleBlockRange(int firstBlock, int lastBlock, int breadcrumbAreaHeight) {
            this.firstBlock = firstBlock;
            this.lastBlock = lastBlock;
            this.breadcrumbAreaHeight = breadcrumbAreaHeight;
        }
    }
} 