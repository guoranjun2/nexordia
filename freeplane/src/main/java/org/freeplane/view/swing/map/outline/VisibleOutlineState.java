package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class VisibleOutlineState {
    private final TreeNode root;
    private List<TreeNode> visibleNodes = new ArrayList<>();
    private final Map<String, Integer> indexById = new HashMap<>();
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
        indexById.clear();
        buildVisibleList(root);
        // build index map by id for O(1) lookups
        for (int i = 0; i < visibleNodes.size(); i++) {
            TreeNode n = visibleNodes.get(i);
            if (n != null) indexById.put(n.getId(), i);
        }
    }

    private void buildVisibleList(TreeNode node) {
        visibleNodes.add(node);
        if (node.isExpanded()) {
            for (TreeNode child : node.getChildren()) {
                buildVisibleList(child);
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
        if (node == null) return -1;
        Integer idx = indexById.get(node.getId());
        return idx != null ? idx : -1;
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

    TreeNode getNodeAtVisibleIndex(int index) {
        if (index < 0 || index >= visibleNodes.size()) return null;
        return visibleNodes.get(index);
    }

    String getNodeIdAtVisibleIndex(int index) {
        TreeNode f = getNodeAtVisibleIndex(index);
        return f != null ? f.getId() : null;
    }
}
