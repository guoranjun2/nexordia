package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.awt.Container;

import javax.swing.FocusManager;
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
        int currentLevel = node.getMaxExpansionLevel();
        node.applyExpansionLevel(currentLevel + 1);
        refreshAfterExpansionChange();
    }

    void reduceNodeExpansion(TreeNode node) {
        int currentLevel = node.getMaxExpansionLevel();
        if (currentLevel > 0 && (currentLevel != 1 || node.getLevel() != 0)) {
            node.applyExpansionLevel(currentLevel - 1);
            refreshAfterExpansionChange();
        }
    }

    private void refreshAfterExpansionChange() {
    	final Component focusOwner = FocusManager.getCurrentManager().getFocusOwner();
    	final Container outlinePane = outlinePane();
    	final boolean wasFocused = (focusOwner instanceof NodeButton) && outlinePane != null && SwingUtilities.isDescendingFrom(focusOwner, outlinePane);
    	treePanel.updateVisibleNodes();
    	SwingUtilities.invokeLater(() -> {
    		if(wasFocused)
    			treePanel.focusSelectionButton(true);
    		else
    			treePanel.synchronizeSelectionButton(false);
    	});

    }

	private Container outlinePane() {
		return SwingUtilities.getAncestorOfClass(OutlinePane.class, treePanel);
	}
}
