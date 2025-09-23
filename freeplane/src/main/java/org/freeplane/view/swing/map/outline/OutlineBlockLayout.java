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
    private int cachedMaxWidth = 0;

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

    void createVisibleBlocks(JPanel owner, OutlineVisibleBlockRange range, int panelWidth) {
        for (int b = range.getFirstBlock(); b <= range.getLastBlock(); b++) {
            if (!blockCache.has(b))
                createBlock(owner, b, panelWidth);
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
        owner.setPreferredSize(new Dimension(cachedMaxWidth, height));

        for (BlockPanel panel : blockCache.values()) {
            Dimension currentSize = panel.getSize();
            panel.setSize(cachedMaxWidth, currentSize.height);
        }
    }

    private void createBlock(JPanel owner, int blockIndex, int panelWidth) {
        int start = blockIndex * blockSize;
        int end = Math.min(start + blockSize, visibleState.getVisibleNodeCount());

        List<TreeNode> blockNodes = new ArrayList<>();
        for (int i = start; i < end; i++) {
            TreeNode n = visibleState.getNodeAtVisibleIndex(i);
            if (n != null) blockNodes.add(n);
        }
        BlockPanel bp = new BlockPanel(blockNodes, geometry.rowHeight, (ScrollableTreePanel) owner, ((ScrollableTreePanel) owner).getOutlineSelection());
        Rectangle bounds = nodePositioning.calculateBlockBounds(blockIndex, blockSize, panelWidth);
        bp.setBounds(bounds);
        owner.add(bp);
        blockCache.put(blockIndex, bp);
        for (Component comp : bp.getComponents()) {
            if (comp instanceof JButton) {
                int rightEdge = comp.getX() + comp.getWidth();
                if (rightEdge > cachedMaxWidth) cachedMaxWidth = rightEdge;
            }
        }
    }

    void recordButtonRightEdge(int rightEdge) {
        if (rightEdge > cachedMaxWidth) cachedMaxWidth = rightEdge;
    }

    void resetCachedMaxWidth() {
        cachedMaxWidth = 0;
    }

    void removeBlocksOutsideRange(JPanel owner, OutlineVisibleBlockRange range) {
        java.util.List<Integer> toRemove = new java.util.ArrayList<>();
        for (int idx : blockCache.keySet()) {
            if (idx < range.getFirstBlock() || idx > range.getLastBlock()) {
                BlockPanel p = blockCache.get(idx);
                if (p != null) owner.remove(p);
                toRemove.add(idx);
            }
        }
        for (int idx : toRemove) blockCache.remove(idx);
    }
}
