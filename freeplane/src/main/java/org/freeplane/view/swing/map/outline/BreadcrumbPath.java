package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BreadcrumbPath {
    private OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;
	private final OutlineSelection outlineSelection;

    BreadcrumbPath(OutlineGeometry geometry, VisibleOutlineState visibleState, OutlineSelection outlineSelection) {
        this.geometry = geometry;
        this.visibleState = visibleState;
		this.outlineSelection = outlineSelection;
    }

    void updateGeometry(OutlineGeometry geometry) {
        this.geometry = geometry;
    }

    BreadcrumbState calculateBreadcrumbStateForIndex(int firstVisibleNodeIndex) {
        int count = visibleState.getVisibleNodeCount();
        if (count == 0) {
            return null;
        }
        int clampedIndex = Math.max(0, Math.min(firstVisibleNodeIndex, count - 1));
        TreeNode breadcrumbTargetNode = visibleState.getNodeAtVisibleIndex(clampedIndex);
        List<TreeNode> newBreadcrumbNodes = collectBreadcrumbNodes(breadcrumbTargetNode);
        int newBreadcrumbHeight = newBreadcrumbNodes.size() * geometry.rowHeight;
        return new BreadcrumbState(newBreadcrumbNodes, newBreadcrumbHeight, clampedIndex);
    }

    private List<TreeNode> collectBreadcrumbNodes(TreeNode fromNode) {
    	List<TreeNode> breadcrumbNodes = new ArrayList<>();
        TreeNode current = fromNode.getParent();
        while (current != null) {
            breadcrumbNodes.add(current);
            current = current.getParent();
        }
        Collections.reverse(breadcrumbNodes);
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
