package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class VisibleOutlineState {
    private final TreeNode root;
    private List<FlatNode> visibleNodes = new ArrayList<>();
    private int breadcrumbAreaHeight = 0;
    private TreeNode hoveredNode;
    private String firstVisibleNodeId;

    VisibleOutlineState(TreeNode root) {
        this.root = root;
        this.hoveredNode = root;
        updateVisibleNodes();
    }

    void updateVisibleNodes() {
        visibleNodes.clear();
        buildVisibleList(root, 0);
    }

    private void buildVisibleList(TreeNode node, int depth) {
        visibleNodes.add(new FlatNode(node, depth));
        if (node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                buildVisibleList(child, depth + 1);
            }
        }
    }

    int getVisibleNodeCount() {
        return visibleNodes.size();
    }

    String getFirstVisibleNodeId() {
        return firstVisibleNodeId;
    }

    void setFirstVisibleNodeId(String id) {
        this.firstVisibleNodeId = id;
    }

    int findNodeIndexInVisibleList(TreeNode node) {
        for (int i = 0; i < visibleNodes.size(); i++) {
            if (visibleNodes.get(i).node == node) {
                return i;
            }
        }
        return -1;
    }

    boolean isNodeInBreadcrumbArea(TreeNode node, int rowHeight) {
        int nodeIndex = findNodeIndexInVisibleList(node);
        int breadcrumbNodeCount = breadcrumbAreaHeight / rowHeight;
        return nodeIndex >= 0 && nodeIndex < breadcrumbNodeCount;
    }

    int getBreadcrumbAreaHeight() {
        return breadcrumbAreaHeight;
    }

    void setBreadcrumbAreaHeight(int height) {
        this.breadcrumbAreaHeight = height;
    }

    TreeNode getHoveredNode() {
        return hoveredNode;
    }

    void setHoveredNode(TreeNode node) {
        this.hoveredNode = node;
    }

    FlatNode getFlatNode(TreeNode node) {
        for (FlatNode flat : visibleNodes) {
            if (flat.node == node) {
                return flat;
            }
        }
        return null;
    }

    FlatNode getFlatNodeAtIndex(int index) {
        if (index < 0 || index >= visibleNodes.size()) return null;
        return visibleNodes.get(index);
    }

    String getNodeIdAtVisibleIndex(int index) {
        FlatNode f = getFlatNodeAtIndex(index);
        return f != null ? f.node.getId() : null;
    }
}
