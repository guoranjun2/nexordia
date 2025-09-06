package org.freeplane.view.swing.map.outline;

import java.util.List;

class BreadcrumbState {
    private final List<TreeNode> breadcrumbNodes;
    private final int breadcrumbHeight;
    private final int firstVisibleNodeIndex;

    BreadcrumbState(List<TreeNode> breadcrumbNodes, int breadcrumbHeight, int levelReductionFirstVisibleNodeIndex) {
        this.breadcrumbNodes = java.util.Collections.unmodifiableList(breadcrumbNodes);
        this.breadcrumbHeight = breadcrumbHeight;
        this.firstVisibleNodeIndex = levelReductionFirstVisibleNodeIndex;
    }

    List<TreeNode> getBreadcrumbNodes() { return breadcrumbNodes; }
    int getBreadcrumbHeight() { return breadcrumbHeight; }
    int getFirstVisibleNodeIndex() { return firstVisibleNodeIndex; }
}
