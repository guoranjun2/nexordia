package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;

class NodePositioning {
    private final TreeNode root;
    private final OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;
    
    public NodePositioning(TreeNode root, OutlineGeometry geometry, VisibleOutlineState visibleState) {
        this.root = root;
        this.geometry = geometry;
        this.visibleState = visibleState;
    }
    
    public int calculateNodeDepth(TreeNode node) {
        int depth = 0;
        TreeNode current = node;
        while (current != root) {
            current = current.parent;
            depth++;
        }
        return depth;
    }
    
    public Point calculateContentButtonPosition(FlatNode flatNode, int visibleButtonIndex) {
        int x = geometry.calculateTextButtonX(flatNode.depth);
        int y = visibleButtonIndex * geometry.rowHeight;
        return new Point(x, y);
    }
    
    public Point calculateNavigationButtonPosition(TreeNode node, boolean isBreadcrumb, int rowIndex, int breadcrumbAreaHeight) {
        int y, depth, baseX;
        
        if (isBreadcrumb) {
            y = rowIndex * geometry.rowHeight;
            depth = calculateNodeDepth(node);
            int textButtonX = geometry.calculateTextButtonX(depth);
            baseX = Math.max(0, textButtonX - geometry.navButtonsTotalWidth);
        } else {
            FlatNode flatNode = visibleState.getFlatNode(node);
            if (flatNode == null) return null;
            
            int nodeIndex = visibleState.findNodeIndexInVisibleList(node);
            int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
            int contentAreaIndex = nodeIndex - breadcrumbNodeCount;
            
            y = breadcrumbAreaHeight + contentAreaIndex * geometry.rowHeight;
            depth = flatNode.depth;
            baseX = geometry.calculateNavigationButtonBaseX(flatNode);
        }
        
        return new Point(baseX, y);
    }
    
    public Point calculateSelectionIconPosition(TreeNode node, Rectangle buttonBounds) {
        FlatNode flatNode = visibleState.getFlatNode(node);
        if (flatNode == null) return null;
        
        int iconX;
        if (flatNode.depth == 0) {
            iconX = Math.max(0, buttonBounds.x - geometry.iconDiameter);
        } else {
            iconX = buttonBounds.x - geometry.iconDiameter;
        }
        
        int iconY = buttonBounds.y + (buttonBounds.height - geometry.iconDiameter) / 2;
        return new Point(iconX, iconY);
    }
    
    public Point calculateViewportPosition(int startFromNodeIndex, int breadcrumbAreaHeight) {
        int targetY = (startFromNodeIndex * geometry.rowHeight) - breadcrumbAreaHeight;
        targetY = Math.max(0, targetY);
        return new Point(0, targetY);
    }
    
    public Rectangle calculateBlockBounds(int blockIndex, int blockSize, int breadcrumbAreaHeight, int panelWidth) {
        int start = blockIndex * blockSize;
        int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
        
        int visibleStart = Math.max(start, breadcrumbNodeCount);
        int end = Math.min(start + blockSize, visibleState.getVisibleNodeCount());
        int visibleNodesInBlock = end - visibleStart;
        
        int blockY = breadcrumbAreaHeight + (visibleStart - breadcrumbNodeCount) * geometry.rowHeight;
        int blockHeight = visibleNodesInBlock * geometry.rowHeight;
        
        return new Rectangle(0, blockY, panelWidth, blockHeight);
    }
    
    public Rectangle calculateBreadcrumbPanelBounds(int viewportWidth, int breadcrumbAreaHeight) {
        return new Rectangle(0, 0, viewportWidth, breadcrumbAreaHeight);
    }
    
    public boolean isNodeInContentArea(TreeNode node, int breadcrumbAreaHeight) {
        return !visibleState.isNodeInBreadcrumbArea(node, geometry.rowHeight);
    }
    
    public int calculateFirstVisibleNodeIndex(Rectangle viewRect, int breadcrumbAreaHeight) {
        int effectiveViewportY = viewRect.y + breadcrumbAreaHeight;
        return Math.max(0, (effectiveViewportY + geometry.rowHeight/2 - 1) / geometry.rowHeight);
    }
} 