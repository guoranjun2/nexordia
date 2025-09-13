package org.freeplane.view.swing.map.outline;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JScrollPane;

class OutlineController implements OutlineActionTarget {
    private final ScrollableTreePanel treePanel;
    @SuppressWarnings("unused")
    private final JScrollPane scrollPane;

    OutlineController(ScrollableTreePanel treePanel,  JScrollPane scrollPane) {
        this.treePanel = treePanel;
        this.scrollPane = scrollPane;
    }

    int getRowHeight() {
        return treePanel.getRowHeight();
    }

    int getViewportWidth() {
        return treePanel.getViewportWidth();
    }

    int calcTextButtonX(int level) {
        return treePanel.calcTextButtonX(level);
    }

    int calculateNodeLevel(TreeNode node) {
        return treePanel.calculateNodeLevel(node);
    }

    void toggleNodeExpansion(TreeNode node) {
        if (node == null) return;
        treePanel.setSelectedNode(node, true);
        treePanel.toggleExpandSelected();
    }

    @Override
    public void toggleExpandSelected() { treePanel.toggleExpandSelected(); }

    void selectNode(TreeNode node, boolean requestFocus) {
        if (node == null) return;
        treePanel.setSelectedNode(node, requestFocus);
    }

    @Override public void navigateUp() { treePanel.navigateUp(); }
    @Override public void navigateDown() { treePanel.navigateDown(); }
    @Override public void navigatePageUp() { treePanel.navigatePageUp(); }
    @Override public void navigatePageDown() { treePanel.navigatePageDown(); }
    @Override public void goToParent() { treePanel.goToParent(); }
    @Override public void goToChild() { treePanel.goToChild(); }
    @Override public void expandSelectedMore() { treePanel.expandSelectedMore(); }
    @Override public void reduceSelectedExpansion() { treePanel.reduceSelectedExpansion(); }

    void attachNavigationNode(TreeNode node, boolean isBreadcrumb, int rowIndex) {
        int currentBreadcrumbHeight = treePanel.getVisibleState().getBreadcrumbAreaHeight();
        treePanel.attachNavigationNode(node, isBreadcrumb, rowIndex, currentBreadcrumbHeight);
    }

    void setBreadcrumbAreaHeight(int height) {
        treePanel.setBreadcrumbAreaHeight(height);
    }

    boolean isNodeInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return treePanel.isNodeInBreadcrumbPath(node, breadcrumbNodes);
    }

    int findNodeIndexInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return treePanel.findNodeIndexInBreadcrumbPath(node, breadcrumbNodes);
    }

    Icon getSelectionIcon() {
        return treePanel.getSelectionIcon();
    }

    Point calculateSelectionIconPosition(Rectangle bounds) {
        return treePanel.getNodePositioning().calculateSelectionIconPosition(bounds);
    }

    TreeNode getHoveredNode() {
        return treePanel.getVisibleState().getHoveredNode();
    }

    boolean areNavButtonsVisible() {
        return treePanel.areNavButtonsVisible();
    }
}
