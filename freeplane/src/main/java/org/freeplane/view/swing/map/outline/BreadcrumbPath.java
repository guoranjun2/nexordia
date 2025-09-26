package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BreadcrumbPath {
    private OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;
	private final OutlineSelection outlineSelection;

    BreadcrumbPath(VisibleOutlineState visibleState, OutlineSelection outlineSelection) {
        this.visibleState = visibleState;
		this.outlineSelection = outlineSelection;
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
}
