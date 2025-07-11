package org.freeplane.view.swing.map.outline;

import java.util.List;

class BreadcrumbState {
    final List<TreeNode> breadcrumbNodes;
    final int breadcrumbHeight;
    final int levelReductionFirstVisibleNodeIndex;

    BreadcrumbState(List<TreeNode> breadcrumbNodes, int breadcrumbHeight, int levelReductionFirstVisibleNodeIndex) {
        this.breadcrumbNodes = breadcrumbNodes;
        this.breadcrumbHeight = breadcrumbHeight;
        this.levelReductionFirstVisibleNodeIndex = levelReductionFirstVisibleNodeIndex;
    }
}