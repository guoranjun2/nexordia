package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class VisibleOutlineState {
    private final TreeNode root;
    private List<FlatNode> visibleNodes = new ArrayList<>();
    private final Map<Integer, BlockPanel> blockPanels = new HashMap<>();
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

    List<FlatNode> getVisibleNodes() {
        return new ArrayList<>(visibleNodes);
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

    void addBlockPanel(int blockIndex, BlockPanel panel) {
        blockPanels.put(blockIndex, panel);
    }

    void clearBlockPanels() {
        blockPanels.clear();
    }

    Map<Integer, BlockPanel> getBlockPanels() {
        return new HashMap<>(blockPanels);
    }

    boolean hasBlockPanel(int blockIndex) {
        return blockPanels.containsKey(blockIndex);
    }

    BlockPanel getBlockPanel(int blockIndex) {
        return blockPanels.get(blockIndex);
    }

    void removeBlockPanel(int blockIndex) {
        blockPanels.remove(blockIndex);
    }

    List<Integer> getBlockPanelIndices() {
        return new ArrayList<>(blockPanels.keySet());
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
}
