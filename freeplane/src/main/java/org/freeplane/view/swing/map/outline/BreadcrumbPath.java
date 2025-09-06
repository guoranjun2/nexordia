package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BreadcrumbPath {
    private final TreeNode root;
    private final OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;
    private OutlineViewport viewport;

    BreadcrumbPath(TreeNode root, OutlineGeometry geometry, VisibleOutlineState visibleState, OutlineViewport viewport) {
        this.root = root;
        this.geometry = geometry;
        this.visibleState = visibleState;
        this.viewport = viewport;
    }

    void setViewport(OutlineViewport viewport) {
        this.viewport = viewport;
    }

    BreadcrumbState calculateBreadcrumbState(List<TreeNode> currentBreadcrumbNodes) {
        List<FlatNode> visibleNodes = visibleState.getVisibleNodes();
        if (visibleNodes.isEmpty()) {
            return null;
        }

        int currentBreadcrumbHeight = visibleState.getBreadcrumbAreaHeight();
        int firstFullyVisibleNodeIndex = viewport.calculateFirstVisibleNodeIndex();

        if (firstFullyVisibleNodeIndex >= visibleNodes.size()) {
            return null;
        }

        TreeNode firstFullyVisibleNode = visibleNodes.get(firstFullyVisibleNodeIndex).node;

        if (firstFullyVisibleNode == root) {
            if (currentBreadcrumbHeight == 0) {
                return null;
            } else {
                return new BreadcrumbState(Collections.emptyList(), 0, 0);
            }
        }

        TreeNode lastCurrentBreadcrumbNode = currentBreadcrumbNodes.isEmpty() ? null :
                                           currentBreadcrumbNodes.get(currentBreadcrumbNodes.size() - 1);

        if (firstFullyVisibleNode.getParent() == lastCurrentBreadcrumbNode) {
            return null;
        }

        List<TreeNode> newBreadcrumbNodes = collectBreadcrumbNodes(firstFullyVisibleNode);
        int newBreadcrumbHeight = newBreadcrumbNodes.size() * geometry.rowHeight;
        return new BreadcrumbState(newBreadcrumbNodes, newBreadcrumbHeight, firstFullyVisibleNodeIndex);
    }

    private List<TreeNode> collectBreadcrumbNodes(TreeNode fromNode) {
        List<TreeNode> breadcrumbNodes = new ArrayList<>();
        TreeNode current = fromNode.getParent();
        while (current != null) {
            breadcrumbNodes.add(0, current);
            current = current.getParent();
        }
        return breadcrumbNodes;
    }

    boolean isNodeInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return breadcrumbNodes.contains(node);
    }

    int findNodeIndexInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        for (int i = 0; i < breadcrumbNodes.size(); i++) {
            if (breadcrumbNodes.get(i) == node) {
                return i;
            }
        }
        return -1;
    }
}
