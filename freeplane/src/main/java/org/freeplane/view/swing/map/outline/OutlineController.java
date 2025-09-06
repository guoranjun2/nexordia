package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JScrollPane;

class OutlineController {
    private final ScrollableTreePanel treePanel;
    @SuppressWarnings("unused")
    private final JScrollPane scrollPane;

    OutlineController(ScrollableTreePanel treePanel,  JScrollPane scrollPane) {
        this.treePanel = treePanel;
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
        treePanel.selectOutlineNodeById(id);
    }

    void toggleNodeExpansion(TreeNode node) {
        if (node == null) return;
        treePanel.setSelectedNodeId(node.id);
        treePanel.toggleExpandSelected();
    }

    void navigateUp() { treePanel.navigateUp(); }
    void navigateDown() { treePanel.navigateDown(); }
    void navigatePageUp() { treePanel.navigatePageUp(); }
    void navigatePageDown() { treePanel.navigatePageDown(); }
    void goToParent() { treePanel.goToParent(); }
    void goToChild() { treePanel.goToChild(); }
    void expandSelectedMore() { treePanel.expandSelectedMore(); }
    void reduceSelectedExpansion() { treePanel.reduceSelectedExpansion(); }

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
        return treePanel.getOutlineSelection();
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
