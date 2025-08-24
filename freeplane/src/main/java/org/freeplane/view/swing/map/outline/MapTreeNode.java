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
public class MapTreeNode extends TreeNode implements INodeView {
    
    private final NodeModel nodeModel;
    private final OutlinePane outlinePane;
    
    public MapTreeNode(NodeModel nodeModel, OutlinePane outlinePane) {
        super(getNodeText(nodeModel), nodeModel.getID());
        this.nodeModel = nodeModel;
        this.outlinePane = outlinePane;
    }
    
    private static String getNodeText(NodeModel nodeModel) {
        return TextController.getController().getShortPlainText(nodeModel);
    }
    
    public NodeModel getNodeModel() {
        return nodeModel;
    }
    
    @Override
    public void nodeChanged(NodeChangeEvent event) {
        if (event.getNode() == nodeModel) {
            // Update the title when node text changes
            String newText = getNodeText(nodeModel);
            // Since title is final, we need to update via the parent TreeNode mechanism
            // Demo has no refresh calls - keep it simple for now
        }
    }
    
    @Override
    public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
        if (parent == nodeModel) {
            // Create new MapTreeNode for the inserted child
            MapTreeNode childTreeNode = new MapTreeNode(child, outlinePane);
            
            // Register the new node as a listener
            child.addViewer(childTreeNode);
            
            // Add to our children at the correct index
            if (newIndex < children.size()) {
                children.add(newIndex, childTreeNode);
            } else {
                children.add(childTreeNode);
            }
            childTreeNode.parent = this;
            
            // Demo has no refresh calls - keep it simple for now
        }
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
            
            // Demo has no refresh calls - keep it simple for now
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
    public void cleanupListeners() {
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
    public String getCurrentText() {
        return getNodeText(nodeModel);
    }
}