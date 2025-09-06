package org.freeplane.view.swing.map.outline;

import javax.swing.SwingUtilities;

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

    private void refreshAfterExpansionChange() {
        treePanel.updateVisibleNodes();
        SwingUtilities.invokeLater(() -> treePanel.synchronizeSelectionButton(false));
    }
}
