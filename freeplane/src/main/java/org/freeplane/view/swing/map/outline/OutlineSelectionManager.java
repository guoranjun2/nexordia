package org.freeplane.view.swing.map.outline;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OutlineSelectionManager {
    private final Map<String, String> lastSelectedChildByParent = new HashMap<>();

    void onSelected(TreeNode node) {
        if (node != null && node.getParent() != null) {
            lastSelectedChildByParent.put(node.getParent().getId(), node.getId());
        }
    }

    TreeNode preferredChild(TreeNode parent) {
        if (parent == null) return null;
        List<TreeNode> children = parent.getChildren();
        if (children.isEmpty()) return null;
        String preferredChildId = lastSelectedChildByParent.get(parent.getId());
        if (preferredChildId != null) {
            for (TreeNode c : children) {
                if (preferredChildId.equals(c.getId())) return c;
            }
        }
        return children.get(0);
    }

    void select(ScrollableTreePanel panel, OutlineSelection selection, TreeNode node, boolean requestFocus) {
        if (node == null) return;
        onSelected(node);
        selection.selectNode(node);
        panel.repaint();
        VisibleOutlineState vs = panel.getVisibleState();
        if (vs.findNodeIndexInVisibleList(node) < 0) {
            TreeNode preservedHoveredNode = vs.getHoveredNode();
            panel.hardResetBlocksPreservingHovered(preservedHoveredNode);
        }

        boolean visible = node != null && (panel.isNodeInBreadcrumbArea(node) || panel.isNodeActuallyVisible(node));
        if (!visible) {
            panel.ensureSelectionVisibleTop();
        }
        panel.focusSelectionButtonLater(requestFocus);
    }
}
