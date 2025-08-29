
package org.freeplane.view.swing.map.outline;

import javax.swing.JComponent;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.view.swing.map.MapView;

/**
 * Factory for creating tree representations of mind map nodes.
 */
class NodeTreeFactory {

    /**
     * Creates a TreeNode hierarchy from the current active map.
     *
     * @param outlinePane the OutlinePane that will display the tree (for refresh callbacks)
     * @return root TreeNode representing the map hierarchy, or null if no map is active
     */
    static TreeNode createTreeFromCurrentMap(OutlinePane outlinePane) {
        try {
            
            JComponent mapViewComponent = Controller.getCurrentController()
                    .getMapViewManager().getMapViewComponent();

            if (mapViewComponent instanceof MapView) {
                MapView mapView = (MapView) mapViewComponent;
                return createTreeFromMap(mapView, outlinePane);
            }
        } catch (Exception e) {
            
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
    static TreeNode createTreeFromMap(MapView mapView, OutlinePane outlinePane) {
        if (mapView == null || mapView.getMap() == null) {
            return null;
        }

        NodeModel rootNode = mapView.getMap().getRootNode();
        if (rootNode == null) {
            return null;
        }

        
        return createMapTreeNode(rootNode, outlinePane);
    }

    /**
     * Recursively creates MapTreeNode hierarchy from NodeModel hierarchy.
     *
     * @param nodeModel the NodeModel to convert
     * @param outlinePane the OutlinePane for refresh callbacks
     * @return MapTreeNode representing this node and its children
     */
    private static MapTreeNode createMapTreeNode(NodeModel nodeModel, OutlinePane outlinePane) {
        
        MapTreeNode treeNode = new MapTreeNode(nodeModel, outlinePane);

        
        nodeModel.addViewer(treeNode);

        
        for (NodeModel childNode : nodeModel.getChildren()) {
            MapTreeNode childTreeNode = createMapTreeNode(childNode, outlinePane);
            treeNode.addChild(childTreeNode);
        }

        return treeNode;
    }

    /**
     * Cleanup all listeners for a tree hierarchy.
     * Call this when replacing or destroying a tree to prevent memory leaks.
     *
     * @param rootTreeNode the root of the tree to cleanup
     */
    static void cleanupTree(TreeNode rootTreeNode) {
        if (rootTreeNode instanceof MapTreeNode) {
            ((MapTreeNode) rootTreeNode).cleanupListeners();
        }
    }
}