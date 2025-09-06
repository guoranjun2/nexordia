package org.freeplane.view.swing.map.outline;

import javax.swing.SwingUtilities;

class ExpansionControls {
    private final ScrollableTreePanel treePanel;

    ExpansionControls(ScrollableTreePanel treePanel) {
        this.treePanel = treePanel;
    }

    void expandNode(TreeNode node) {
        node.applyExpansionLevel(1);
        refreshAfterExpansionChange();
    }

    void collapseNode(TreeNode node) {
        node.applyExpansionLevel(0);
        refreshAfterExpansionChange();
    }

    void expandNodeMore(TreeNode node) {
        int currentLevel = node.getMaxExpansionDepth();
        node.applyExpansionLevel(currentLevel + 1);
        refreshAfterExpansionChange();
    }

    void reduceNodeExpansion(TreeNode node) {
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
