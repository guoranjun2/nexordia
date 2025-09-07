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

    boolean isNodeInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return treePanel.isNodeInBreadcrumbPath(node, breadcrumbNodes);
    }

    int findNodeIndexInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return treePanel.findNodeIndexInBreadcrumbPath(node, breadcrumbNodes);
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
