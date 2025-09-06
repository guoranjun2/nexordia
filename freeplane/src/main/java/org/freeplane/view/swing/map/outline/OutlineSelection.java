package org.freeplane.view.swing.map.outline;

class OutlineSelection {
    private TreeNode selectedNode;

    public OutlineSelection(TreeNode root) {
        this.selectedNode = root;
    }

    public void selectNode(TreeNode node) {
        if (node != null && node != selectedNode) {
        	selectedNode = node;
        }
    }

    public String getSelectedNodeId() {
        return selectedNode.getId();
    }

    public TreeNode getSelectedNode() {
        return selectedNode;
    }

    private boolean isSelected(String nodeId) {
        return selectedNode != null && selectedNode.getId().equals(nodeId);
    }

    public boolean isSelected(TreeNode node) {
        return node != null && isSelected(node.getId());
    }
}