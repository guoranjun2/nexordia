package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.Icon;

class OutlineController {
    private final ScrollableTreePanel treePanel;
    private final BreadcrumbPanel breadcrumbPanel;
    @SuppressWarnings("unused")
    private final JScrollPane scrollPane;

    OutlineController(ScrollableTreePanel treePanel, BreadcrumbPanel breadcrumbPanel, JScrollPane scrollPane) {
        this.treePanel = treePanel;
        this.breadcrumbPanel = breadcrumbPanel;
        this.scrollPane = scrollPane;
    }

    void refreshAll() {
        treePanel.updateVisibleNodes();
    }

    void refreshFromStartIndex(int startFromNodeIndex) {
        treePanel.updateVisibleBlocks(startFromNodeIndex);
    }

    void rebuildFromAnchor(TreeNode anchorNode) {
        treePanel.rebuildFromNode(anchorNode);
    }

    void updateTitle(TreeNode node) {
        treePanel.updateNodeTitle(node);
    }

    void onSelect(TreeNode node) {
        if (node != null)
            treePanel.setSelectedNodeId(node.id);
    }

    void onHover(TreeNode node) {
        if (node != null)
            treePanel.onContentButtonHovered(node);
    }

    int getRowHeight() {
        return treePanel.getRowHeight();
    }

    int getViewportWidth() {
        return treePanel.getViewportWidth();
    }

    int calcTextButtonX(int depth) {
        return treePanel.calcTextButtonX(depth);
    }

    int calculateNodeDepth(TreeNode node) {
        return treePanel.calculateNodeDepth(node);
    }

    void selectNodeById(String id) {
        treePanel.selectNodeById(id);
    }

    void attachNavigationNode(TreeNode node, boolean isBreadcrumb, int rowIndex) {
        int currentBreadcrumbHeight = treePanel.getVisibleState().getBreadcrumbAreaHeight();
        treePanel.attachNavigationNode(node, isBreadcrumb, rowIndex, currentBreadcrumbHeight);
    }

    void setBreadcrumbAreaHeight(int height) {
        treePanel.setBreadcrumbAreaHeight(height);
    }

    boolean isNodeInBreadcrumbArea(TreeNode node) {
        return treePanel.isNodeInBreadcrumbArea(node);
    }

    boolean isNodeInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return treePanel.isNodeInBreadcrumbPath(node, breadcrumbNodes);
    }

    int findNodeIndexInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return treePanel.findNodeIndexInBreadcrumbPath(node, breadcrumbNodes);
    }

    OutlineSelection getSelection() {
        return treePanel.getSelection();
    }

    Icon getSelectionIcon() {
        return treePanel.getSelectionIcon();
    }

    Point calculateSelectionIconPosition(TreeNode node, Rectangle bounds) {
        return treePanel.getNodePositioning().calculateSelectionIconPosition(node, bounds);
    }

    TreeNode getHoveredNode() {
        return treePanel.getVisibleState().getHoveredNode();
    }

    boolean areNavButtonsVisible() {
        return treePanel.areNavButtonsVisible();
    }
}
