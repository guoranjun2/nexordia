
package org.freeplane.view.swing.map.outline;

import org.freeplane.features.map.INodeView;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeDeletionEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import javax.swing.SwingUtilities;

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
            
            String newText = getNodeText(nodeModel);
            setTitle(newText);
            if (outlinePane != null) {
                SwingUtilities.invokeLater(() -> {
                    outlinePane.updateNodeTitle(this);
                });
            }
        }
    }

    @Override
    public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
        if (parent == nodeModel) {
            
            MapTreeNode childTreeNode = createMapTreeNodeRecursively(child, outlinePane);

            
            if (newIndex < children.size()) {
                children.add(newIndex, childTreeNode);
            } else {
                children.add(childTreeNode);
            }
            childTreeNode.parent = this;

            
            if (outlinePane != null) {
                SwingUtilities.invokeLater(() -> {
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
            
            deletedNode.removeViewer(toRemove);

            
            children.remove(toRemove);
            toRemove.parent = null;

            
            toRemove.cleanupListeners();

            
            if (outlinePane != null) {
                SwingUtilities.invokeLater(() -> {
                    outlinePane.rebuildFromNode(this);
                });
            }
        }
    }

    @Override
    public boolean hasStandardLayoutWithRootNode(NodeModel root) {
        return false; 
    }

    @Override
    public boolean isTopOrLeft() {
        return true; 
    }

    /**
     * Recursively cleanup all INodeView listeners for this node and its children.
     * Called when the tree is being destroyed or replaced.
     */
    void cleanupListeners() {
        
        if (nodeModel != null) {
            nodeModel.removeViewer(this);
        }

        
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
