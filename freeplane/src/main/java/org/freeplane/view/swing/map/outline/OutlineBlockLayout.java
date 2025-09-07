package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

class OutlineBlockLayout {
    private final OutlineBlockViewCache blockCache;
    private final VisibleOutlineState visibleState;
    private final OutlineGeometry geometry;
    private final NodePositioning nodePositioning;
    private final int blockSize;

    OutlineBlockLayout(OutlineBlockViewCache blockCache,
                       VisibleOutlineState visibleState,
                       OutlineGeometry geometry,
                       NodePositioning nodePositioning,
                       int blockSize) {
        this.blockCache = blockCache;
        this.visibleState = visibleState;
        this.geometry = geometry;
        this.nodePositioning = nodePositioning;
        this.blockSize = blockSize;
    }

    void clearBlocks(JPanel owner) {
        for (BlockPanel panel : blockCache.values()) {
            owner.remove(panel);
        }
        blockCache.clear();
    }

    void createVisibleBlocks(JPanel owner, OutlineViewport.VisibleBlockRange range, int panelWidth) {
        for (int b = range.getFirstBlock(); b <= range.getLastBlock(); b++) {
            if (!blockCache.has(b))
                createBlock(owner, b, range.getBreadcrumbAreaHeight(), panelWidth);
        }
    }

    void removeBlocksFromBlockIndex(JPanel owner, int startBlock) {
        List<Integer> indices = new ArrayList<>(blockCache.keySet());
        for (int idx : indices) {
            if (idx >= startBlock) {
                BlockPanel p = blockCache.get(idx);
                if (p != null) owner.remove(p);
                blockCache.remove(idx);
            }
        }
    }

    void updatePreferredFromActualBlocks(JPanel owner) {
        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
        int contentNodesCount = Math.max(0, visibleState.getVisibleNodeCount() - breadcrumbNodeCount);
        int height = breadcrumbAreaHeight + (contentNodesCount + 1) * geometry.rowHeight;
        int maxWidth = calculateActualRequiredWidth();
        owner.setPreferredSize(new Dimension(maxWidth, height));

        for (BlockPanel panel : blockCache.values()) {
            Dimension currentSize = panel.getSize();
            panel.setSize(maxWidth, currentSize.height);
        }
    }

    private void createBlock(JPanel owner, int blockIndex, int yOffset, int panelWidth) {
        int start = blockIndex * blockSize;
        int end = Math.min(start + blockSize, visibleState.getVisibleNodeCount());
        int breadcrumbNodeCount = visibleState.getBreadcrumbAreaHeight() / geometry.rowHeight;
        if (end <= breadcrumbNodeCount) return;

        List<TreeNode> blockNodes = new ArrayList<>();
        for (int i = start; i < end; i++) {
            TreeNode n = visibleState.getNodeAtVisibleIndex(i);
            if (n != null) blockNodes.add(n);
        }
        BlockPanel bp = new BlockPanel(blockNodes, start, geometry.rowHeight, (ScrollableTreePanel) owner, breadcrumbNodeCount, ((ScrollableTreePanel) owner).getOutlineSelection());
        Rectangle bounds = nodePositioning.calculateBlockBounds(blockIndex, blockSize, yOffset, panelWidth);
        bp.setBounds(bounds);
        owner.add(bp);
        blockCache.put(blockIndex, bp);
    }

    private int calculateActualRequiredWidth() {
        int maxWidth = 400;
        for (BlockPanel panel : blockCache.values()) {
            Component[] components = panel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JButton) {
                    int rightEdge = comp.getX() + comp.getWidth();
                    maxWidth = Math.max(maxWidth, rightEdge);
                }
            }
        }
        return maxWidth + 20;
    }
}

