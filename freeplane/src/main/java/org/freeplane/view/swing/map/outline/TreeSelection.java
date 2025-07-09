package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.List;

class TreeSelection {
    private String selectedNodeId;
    private final TreeNode root;
    private final List<SelectionListener> listeners = new ArrayList<>();

    public TreeSelection(TreeNode root) {
        this.root = root;
        this.selectedNodeId = root.id;
    }

    public void selectNode(String nodeId) {
        if (nodeId != null && !nodeId.equals(selectedNodeId)) {
            String oldSelection = selectedNodeId;
            selectedNodeId = nodeId;
            notifyListeners(oldSelection, selectedNodeId);
        }
    }

    public String getSelectedNodeId() {
        return selectedNodeId;
    }

    public TreeNode getSelectedNode() {
        return findNodeById(selectedNodeId);
    }

    public boolean isSelected(String nodeId) {
        return selectedNodeId != null && selectedNodeId.equals(nodeId);
    }

    public boolean isSelected(TreeNode node) {
        return node != null && isSelected(node.id);
    }

    public TreeNode findNodeById(String id) {
        return findNodeById(root, id);
    }

    private TreeNode findNodeById(TreeNode node, String id) {
        if (node.id.equals(id)) {
            return node;
        }
        for (TreeNode child : node.children) {
            TreeNode result = findNodeById(child, id);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    public void addSelectionListener(SelectionListener listener) {
        listeners.add(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String oldSelection, String newSelection) {
        for (SelectionListener listener : listeners) {
            listener.selectionChanged(oldSelection, newSelection);
        }
    }

    public interface SelectionListener {
        void selectionChanged(String oldSelection, String newSelection);
    }
} 