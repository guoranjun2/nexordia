package org.freeplane.view.swing.map.outline;

class ExpansionControls {
    private final ScrollableTreePanel treePanel;
    
    public ExpansionControls(ScrollableTreePanel treePanel) {
        this.treePanel = treePanel;
    }
    
    public void expandNode(TreeNode node) {
        node.applyExpansionLevel(1);
        refreshAfterExpansionChange();
    }
    
    public void collapseNode(TreeNode node) {
        node.applyExpansionLevel(0);
        refreshAfterExpansionChange();
    }
    
    public void expandNodeMore(TreeNode node) {
        int currentLevel = node.getMaxExpansionDepth();
        node.applyExpansionLevel(currentLevel + 1);
        refreshAfterExpansionChange();
    }
    
    public void reduceNodeExpansion(TreeNode node) {
        int currentLevel = node.getMaxExpansionDepth();
        if (currentLevel > 0) {
            node.applyExpansionLevel(currentLevel - 1);
            refreshAfterExpansionChange();
        }
    }
    
    public boolean canExpand(TreeNode node) {
        return !node.children.isEmpty() && !node.isExpanded();
    }
    
    public boolean canCollapse(TreeNode node) {
        return node.isExpanded();
    }
    
    public boolean canExpandMore(TreeNode node) {
        return !node.children.isEmpty() && node.isExpanded();
    }
    
    public boolean canReduceExpansion(TreeNode node) {
        return node.getMaxExpansionDepth() > 0;
    }
    
    private void refreshAfterExpansionChange() {
        treePanel.refreshWithBreadcrumbs();
        treePanel.requestFocusInWindow();
    }
} 