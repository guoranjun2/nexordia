package org.freeplane.view.swing.map.outline;

import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;

class ExpansionControls {
    private final ScrollableTreePanel treePanel;
	private final OutlineSelection outlineSelection;

    ExpansionControls(ScrollableTreePanel treePanel, OutlineSelection outlineSelection) {
        this.treePanel = treePanel;
		this.outlineSelection = outlineSelection;
    }

    void expandNode(TreeNode node) {
        node.applyExpansionLevel(1);
        refreshAfterExpansionChange();
    }

    void collapseNode(TreeNode node) {
        node.applyExpansionLevel(0);
        selectParentIfNeeded();
        refreshAfterExpansionChange();
    }

    void expandNodeMore(TreeNode node) {
        int currentLevel = node.getMaxExpansionLevel();
        node.applyExpansionLevel(currentLevel + 1);
        refreshAfterExpansionChange();
    }

    void reduceNodeExpansion(TreeNode node) {
        int currentLevel = node.getMaxExpansionLevel();
		final int minimalLevel = ResourceController.getResourceController().getIntProperty("minimalFoldableOutlineLevel", 1);
        if (currentLevel > minimalLevel - node.getLevel()) {
            node.applyExpansionLevel(currentLevel - 1);
            selectParentIfNeeded();
            refreshAfterExpansionChange();
        }
    }

	private void selectParentIfNeeded() {
		final TreeNode selectedNode = outlineSelection.getSelectedNode();
		if(selectedNode != null && ! selectedNode.isVisible())
			outlineSelection.selectNode(selectedNode.getParent());
	}

    private void refreshAfterExpansionChange() {
    	final boolean wasFocused = treePanel.isNodeButtonFocused();
    	treePanel.updateVisibleNodes();
    	SwingUtilities.invokeLater(() -> {
    		if(wasFocused)
    			treePanel.focusSelectionButtonLater(true);
    		else
    			treePanel.synchronizeSelectionButton(false);
    	});

    }

}
