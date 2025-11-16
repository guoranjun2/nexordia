package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.swing.JPanel;

final class BreadcrumbLayout {
    private final BreadcrumbPanel breadcrumbPanel;
    private final VisibleOutlineNodes visibleNodes;
    private final NavigationButtons navigationButtons;
    private final OutlineSelection outlineSelection;
    private final Supplier<Boolean> selectionDrivenMode;
    private final Predicate<TreeNode> isNodeInBreadcrumbArea;
    private final Consumer<Integer> breadcrumbHeightUpdater;
    private final JPanel blockPanel;
    private OutlineSelectionBridge selectionBridge;

    BreadcrumbLayout(BreadcrumbPanel breadcrumbPanel,
                     VisibleOutlineNodes visibleNodes,
                     NavigationButtons navigationButtons,
                     OutlineSelection outlineSelection,
                     Supplier<Boolean> selectionDrivenMode,
                     Predicate<TreeNode> isNodeInBreadcrumbArea,
                     Consumer<Integer> breadcrumbHeightUpdater,
                     JPanel blockPanel) {
        this.breadcrumbPanel = breadcrumbPanel;
        this.visibleNodes = visibleNodes;
        this.navigationButtons = navigationButtons;
        this.outlineSelection = outlineSelection;
        this.selectionDrivenMode = selectionDrivenMode;
        this.isNodeInBreadcrumbArea = isNodeInBreadcrumbArea;
        this.breadcrumbHeightUpdater = breadcrumbHeightUpdater;
        this.blockPanel = blockPanel;
    }

    void setSelectionBridge(OutlineSelectionBridge selectionBridge) {
        this.selectionBridge = selectionBridge;
    }

    void updateForSelection() {
        applyState(calculateStateForSelection());
    }

    void updateForFirstVisibleIndex(int index) {
        applyState(calculateStateForIndex(index));
    }

    List<TreeNode> calculateState(int targetFirstIndex) {
        return selectionDrivenMode.get()
                ? calculateStateForSelection()
                : calculateStateForIndex(targetFirstIndex);
    }

    void applyState(List<TreeNode> breadcrumbState) {
        if (breadcrumbState != null) {
            breadcrumbPanel.update(breadcrumbState, false);
        }
        else {
            breadcrumbHeightUpdater.accept(0);
            breadcrumbPanel.removeAll();
            breadcrumbPanel.revalidate();
            breadcrumbPanel.repaint();
        }
        reattachNavigationButtons();
    }

    void reattachNavigationButtons() {
        TreeNode hoveredNode = visibleNodes.getHoveredNode();
        if (hoveredNode == null || hoveredNode.getChildren().isEmpty()) {
            return;
        }

        boolean inBreadcrumb = visibleNodes.isHoveredNodeContainedInBreadcrumb() && isNodeInBreadcrumbArea.test(hoveredNode);
        if (inBreadcrumb) {
            List<TreeNode> breadcrumbNodes = breadcrumbPanel.getCurrentBreadcrumbNodes();
            int rowIndex = breadcrumbNodes.indexOf(hoveredNode);
            if (rowIndex >= 0) {
                navigationButtons.attachToNode(hoveredNode, breadcrumbPanel, rowIndex);
            }
            return;
        }

        int nodeIndex = visibleNodes.findNodeIndexInVisibleList(hoveredNode);
		navigationButtons.attachToNode(hoveredNode, blockPanel, nodeIndex);
    }

    private List<TreeNode> calculateStateForIndex(int firstVisibleNodeIndex) {
        TreeNode breadcrumbTargetNode = visibleNodes.getNodeAtVisibleIndex(firstVisibleNodeIndex);
        if (breadcrumbTargetNode == null) {
            return null;
        }
        return collectBreadcrumbNodes(breadcrumbTargetNode);
    }

    private List<TreeNode> calculateStateForSelection() {
        TreeNode selected = outlineSelection.getSelectedNode();
        if (selected == null) {
            return null;
        }
        List<TreeNode> nodes = collectBreadcrumbNodes(selected);
        nodes.add(selected);
        if (selectionBridge != null && outlineSelection.showsExtendedBreadcrumb()) {
            Collection<? extends TreeNode> extraNodes = selectionBridge.collectNodesToSelection(selected);
            if(extraNodes.isEmpty())
            	outlineSelection.setShowsExtendedBreadcrumb(false);
			nodes.addAll(extraNodes);
        }
        return nodes;
    }

    private List<TreeNode> collectBreadcrumbNodes(TreeNode fromNode) {
        List<TreeNode> breadcrumbNodes = new ArrayList<>();
        TreeNode current = fromNode.getParent();
        while (current != null) {
            breadcrumbNodes.add(current);
            current = current.getParent();
        }
        java.util.Collections.reverse(breadcrumbNodes);
        return breadcrumbNodes;
    }
}
