/*
 * Created on 23 Aug 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import javax.swing.JComponent;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.view.swing.map.MapView;

/**
 * Factory for creating tree representations of mind map nodes.
 */
public class NodeTreeFactory {
    
    /**
     * Creates a TreeNode hierarchy from the current active map.
     * 
     * @param outlinePane the OutlinePane that will display the tree (for refresh callbacks)
     * @return root TreeNode representing the map hierarchy, or null if no map is active
     */
    public static TreeNode createTreeFromCurrentMap(OutlinePane outlinePane) {
        try {
            // Get the current map view
            JComponent mapViewComponent = Controller.getCurrentController()
                    .getMapViewManager().getMapViewComponent();
            
            if (mapViewComponent instanceof MapView) {
                MapView mapView = (MapView) mapViewComponent;
                return createTreeFromMap(mapView, outlinePane);
            }
        } catch (Exception e) {
            // Fallback to demo data if anything goes wrong
            System.err.println("Failed to create tree from current map: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Creates a TreeNode hierarchy from the specified MapView.
     * 
     * @param mapView the MapView to convert
     * @param outlinePane the OutlinePane that will display the tree
     * @return root TreeNode representing the map hierarchy
     */
    public static TreeNode createTreeFromMap(MapView mapView, OutlinePane outlinePane) {
        if (mapView == null || mapView.getMap() == null) {
            return null;
        }
        
        NodeModel rootNode = mapView.getMap().getRootNode();
        if (rootNode == null) {
            return null;
        }
        
        // Create the tree recursively
        return createMapTreeNode(rootNode, outlinePane);
    }
    
    /**
     * Recursively creates TreeNode hierarchy from NodeModel hierarchy.
     * Use simple TreeNode objects like the demo to avoid virtual scrolling issues.
     * 
     * @param nodeModel the NodeModel to convert
     * @param outlinePane unused, kept for compatibility
     * @return TreeNode representing this node and its children
     */
    private static TreeNode createMapTreeNode(NodeModel nodeModel, OutlinePane outlinePane) {
        // Create simple TreeNode like the demo - no listeners for now
        String nodeText = org.freeplane.features.text.TextController.getController().getShortPlainText(nodeModel);
        TreeNode treeNode = new TreeNode(nodeText, nodeModel.getID());
        
        // Recursively create children
        for (NodeModel childNode : nodeModel.getChildren()) {
            TreeNode childTreeNode = createMapTreeNode(childNode, outlinePane);
            treeNode.addChild(childTreeNode);
        }
        
        return treeNode;
    }
    
    /**
     * Cleanup all listeners for a tree hierarchy.
     * No cleanup needed for simple TreeNode objects.
     * 
     * @param rootTreeNode the root of the tree to cleanup
     */
    public static void cleanupTree(TreeNode rootTreeNode) {
        // No cleanup needed for simple TreeNode objects
    }
}