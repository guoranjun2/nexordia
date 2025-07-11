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
    
    public VisibleOutlineState(TreeNode root) {
        this.root = root;
        this.hoveredNode = root;
    }
    
    public void updateVisibleNodes() {
        visibleNodes.clear();
        buildVisibleList(root, 0);
    }
    
    private void buildVisibleList(TreeNode node, int depth) {
        visibleNodes.add(new FlatNode(node, depth));
        if (node.isExpanded()) {
            for (TreeNode child : node.children) {
                buildVisibleList(child, depth + 1);
            }
        }
    }
    
    public List<FlatNode> getVisibleNodes() {
        return new ArrayList<>(visibleNodes);
    }
    
    public int getVisibleNodeCount() {
        return visibleNodes.size();
    }
    
    public int findNodeIndexInVisibleList(TreeNode node) {
        for (int i = 0; i < visibleNodes.size(); i++) {
            if (visibleNodes.get(i).node == node) {
                return i;
            }
        }
        return -1;
    }
    
    public boolean isNodeInBreadcrumbArea(TreeNode node, int rowHeight) {
        int nodeIndex = findNodeIndexInVisibleList(node);
        int breadcrumbNodeCount = breadcrumbAreaHeight / rowHeight;
        return nodeIndex >= 0 && nodeIndex < breadcrumbNodeCount;
    }
    
    public void addBlockPanel(int blockIndex, BlockPanel panel) {
        blockPanels.put(blockIndex, panel);
    }
    
    public void clearBlockPanels() {
        blockPanels.clear();
    }
    
    public Map<Integer, BlockPanel> getBlockPanels() {
        return new HashMap<>(blockPanels);
    }
    
    public int getBreadcrumbAreaHeight() {
        return breadcrumbAreaHeight;
    }
    
    public void setBreadcrumbAreaHeight(int height) {
        this.breadcrumbAreaHeight = height;
    }
    
    public TreeNode getHoveredNode() {
        return hoveredNode;
    }
    
    public void setHoveredNode(TreeNode node) {
        this.hoveredNode = node;
    }
    
    public TreeNode getRoot() {
        return root;
    }
    
    public FlatNode getFlatNode(TreeNode node) {
        for (FlatNode flat : visibleNodes) {
            if (flat.node == node) {
                return flat;
            }
        }
        return null;
    }
    
    public int getMaxDepth() {
        int maxDepth = 0;
        for (FlatNode flat : visibleNodes) {
            maxDepth = Math.max(maxDepth, flat.depth);
        }
        return maxDepth;
    }
} 