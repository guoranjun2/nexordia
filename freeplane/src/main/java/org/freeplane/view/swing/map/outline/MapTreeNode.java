/*
 * Created on 23 Aug 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeDeletionEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;

/**
 * TreeNode that wraps a NodeModel and implements INodeView to receive
 * live updates when the underlying node changes.
 */
class MapTreeNode extends TreeNode implements INodeView {

    private final NodeModel nodeModel;
    private final OutlinePane outlinePane;

    MapTreeNode(NodeModel nodeModel, OutlinePane outlinePane) {
        super(getNodeText(nodeModel), nodeModel.getID());
        this.nodeModel = nodeModel;
        this.outlinePane = outlinePane;
    }

    /**
     * Get current title from the NodeModel (may have changed since creation).
     * Override the final title field behavior for live updates.
     */
    String getCurrentTitle() {
        return getNodeText(nodeModel);
    }

    private static String getNodeText(NodeModel nodeModel) {
        return TextController.getController().getShortPlainText(nodeModel);
    }

    NodeModel getNodeModel() {
        return nodeModel;
    }

    @Override
    public void nodeChanged(NodeChangeEvent event) {
        if (event.getNode() == nodeModel) {
            // Update the title and repaint
            String newText = getNodeText(nodeModel);
            setTitle(newText);
            if (outlinePane != null) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    outlinePane.updateNodeTitle(this);
                });
            }
        }
    }

    @Override
    public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
        if (parent == nodeModel) {
            // Create new MapTreeNode hierarchy recursively
            MapTreeNode childTreeNode = createMapTreeNodeRecursively(child, outlinePane);

            // Add to our children at the correct index
            if (newIndex < children.size()) {
                children.add(newIndex, childTreeNode);
            } else {
                children.add(childTreeNode);
            }
            childTreeNode.parent = this;

            // Incremental rebuild from parent (this) if visible
            if (outlinePane != null) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    outlinePane.rebuildFromNode(this);
                });
            }
        }
    }

    private static MapTreeNode createMapTreeNodeRecursively(NodeModel nodeModel, OutlinePane outlinePane) {
        MapTreeNode treeNode = new MapTreeNode(nodeModel, outlinePane);
        nodeModel.addViewer(treeNode);

        for (NodeModel childNode : nodeModel.getChildren()) {
            MapTreeNode childTreeNode = createMapTreeNodeRecursively(childNode, outlinePane);
            treeNode.addChild(childTreeNode);
        }

        return treeNode;
    }

    @Override
    public void onNodeDeleted(NodeDeletionEvent nodeDeletionEvent) {
        NodeModel deletedNode = nodeDeletionEvent.node;

        // Find and remove the corresponding TreeNode child
        MapTreeNode toRemove = null;
        for (TreeNode child : children) {
            if (child instanceof MapTreeNode) {
                MapTreeNode mapChild = (MapTreeNode) child;
                if (mapChild.nodeModel == deletedNode) {
                    toRemove = mapChild;
                    break;
                }
            }
        }

        if (toRemove != null) {
            // Cleanup: unregister the deleted node as a listener
            deletedNode.removeViewer(toRemove);

            // Remove from children
            children.remove(toRemove);
            toRemove.parent = null;

            // Recursively cleanup child listeners
            toRemove.cleanupListeners();

            // Incremental rebuild from parent (this)
            if (outlinePane != null) {
                javax.swing.SwingUtilities.invokeLater(() -> {
                    outlinePane.rebuildFromNode(this);
                });
            }
        }
    }

    @Override
    public boolean hasStandardLayoutWithRootNode(NodeModel root) {
        return false; // We're not a standard layout
    }

    @Override
    public boolean isTopOrLeft() {
        return true; // Outline is typically on the left
    }

    /**
     * Recursively cleanup all INodeView listeners for this node and its children.
     * Called when the tree is being destroyed or replaced.
     */
    void cleanupListeners() {
        // Unregister ourselves
        if (nodeModel != null) {
            nodeModel.removeViewer(this);
        }

        // Recursively cleanup children
        for (TreeNode child : children) {
            if (child instanceof MapTreeNode) {
                ((MapTreeNode) child).cleanupListeners();
            }
        }
    }

    /**
     * Get the current text of the underlying node (may have changed since creation).
     */
    String getCurrentText() {
        return getNodeText(nodeModel);
    }
}
