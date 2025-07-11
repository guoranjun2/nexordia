package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BreadcrumbPath {
    private final TreeNode root;
    private final OutlineGeometry geometry;
    private final VisibleOutlineState visibleState;
    private OutlineViewport viewport;
    
    public BreadcrumbPath(TreeNode root, OutlineGeometry geometry, VisibleOutlineState visibleState, OutlineViewport viewport) {
        this.root = root;
        this.geometry = geometry;
        this.visibleState = visibleState;
        this.viewport = viewport;
    }
    
    public void setViewport(OutlineViewport viewport) {
        this.viewport = viewport;
    }
    
    public BreadcrumbState calculateBreadcrumbState(List<TreeNode> currentBreadcrumbNodes) {
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

        if (firstFullyVisibleNode.parent == lastCurrentBreadcrumbNode) {
            return null;
        }

        List<TreeNode> newBreadcrumbNodes = collectBreadcrumbNodes(firstFullyVisibleNode);
        int newBreadcrumbHeight = newBreadcrumbNodes.size() * geometry.rowHeight;

        return new BreadcrumbState(newBreadcrumbNodes, newBreadcrumbHeight, firstFullyVisibleNodeIndex);
    }
    
    public List<TreeNode> collectBreadcrumbNodes(TreeNode fromNode) {
        List<TreeNode> breadcrumbNodes = new ArrayList<>();
        TreeNode current = fromNode.parent;
        while (current != null) {
            breadcrumbNodes.add(0, current);
            current = current.parent;
        }
        return breadcrumbNodes;
    }
    
    public List<TreeNode> getPathToRoot(TreeNode node) {
        List<TreeNode> path = new ArrayList<>();
        TreeNode current = node;
        while (current != null) {
            path.add(0, current);
            current = current.parent;
        }
        return path;
    }
    
    public boolean isNodeInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return breadcrumbNodes.contains(node);
    }
    
    public int findNodeIndexInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        for (int i = 0; i < breadcrumbNodes.size(); i++) {
            if (breadcrumbNodes.get(i) == node) {
                return i;
            }
        }
        return -1;
    }
    
    public boolean shouldUpdateBreadcrumb(TreeNode firstVisibleNode, List<TreeNode> currentBreadcrumbNodes) {
        if (firstVisibleNode == root) {
            return visibleState.getBreadcrumbAreaHeight() > 0;
        }
        
        TreeNode lastCurrentBreadcrumbNode = currentBreadcrumbNodes.isEmpty() ? null : 
                                           currentBreadcrumbNodes.get(currentBreadcrumbNodes.size() - 1);
        
        return firstVisibleNode.parent != lastCurrentBreadcrumbNode;
    }
    
    public TreeNode getCommonAncestor(TreeNode node1, TreeNode node2) {
        List<TreeNode> path1 = getPathToRoot(node1);
        List<TreeNode> path2 = getPathToRoot(node2);
        
        TreeNode commonAncestor = null;
        int minLength = Math.min(path1.size(), path2.size());
        
        for (int i = 0; i < minLength; i++) {
            if (path1.get(i) == path2.get(i)) {
                commonAncestor = path1.get(i);
            } else {
                break;
            }
        }
        
        return commonAncestor;
    }
    
    public int calculateBreadcrumbHeight(List<TreeNode> breadcrumbNodes) {
        return breadcrumbNodes.size() * geometry.rowHeight;
    }
    
    public boolean isEmpty(List<TreeNode> breadcrumbNodes) {
        return breadcrumbNodes == null || breadcrumbNodes.isEmpty();
    }
} 