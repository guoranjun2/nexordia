package org.freeplane.view.swing.map.outline;

import java.util.List;

class BreadcrumbState {
    final List<TreeNode> breadcrumbNodes;
    final int breadcrumbHeight;
    final boolean needsScroll;
    final int levelReductionFirstVisibleNodeIndex;

    BreadcrumbState(List<TreeNode> breadcrumbNodes, int breadcrumbHeight, boolean needsScroll, int levelReductionFirstVisibleNodeIndex) {
        this.breadcrumbNodes = breadcrumbNodes;
        this.breadcrumbHeight = breadcrumbHeight;
        this.needsScroll = needsScroll;
        this.levelReductionFirstVisibleNodeIndex = levelReductionFirstVisibleNodeIndex;
    }
} 