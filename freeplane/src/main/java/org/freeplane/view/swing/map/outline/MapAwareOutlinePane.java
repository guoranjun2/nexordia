/*
 * Created on 23 Aug 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import javax.swing.SwingUtilities;

import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.view.swing.map.MapView;

/**
 * OutlinePane that automatically updates its content based on map view changes.
 * Implements the same pattern as BookmarkToolbarPane to listen for map switching.
 */
public class MapAwareOutlinePane extends OutlinePane implements IMapViewChangeListener {
    
    private TreeNode currentRoot;
    private MapView currentMapView;
    
    public MapAwareOutlinePane() {
        super(new TreeNode("Loading...", "loading"));
        
        // Register as a map view change listener
        Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(this);
        
        // Removed auto-launch test frame
        
        // afterViewChange will handle initialization when the first map view change event fires
    }
    
    @Override
    public void afterViewChange(Component oldView, Component newView) {
        SwingUtilities.invokeLater(() -> {
            if (newView instanceof MapView) {
                updateTreeFromMap((MapView) newView);
            } else {
                showNoMapState();
            }
        });
    }
    
    @Override
    public void afterViewClose(Component oldView) {
        SwingUtilities.invokeLater(() -> {
            // Check if there's still an active map
            try {
                Component mapViewComponent = Controller.getCurrentController()
                        .getMapViewManager().getMapViewComponent();
                
                if (mapViewComponent instanceof MapView) {
                    updateTreeFromMap((MapView) mapViewComponent);
                } else {
                    showNoMapState();
                }
            } catch (Exception e) {
                showNoMapState();
            }
        });
    }
    
    @Override
    public void afterViewCreated(Component newView) {
        SwingUtilities.invokeLater(() -> {
            if (newView instanceof MapView) {
                updateTreeFromMap((MapView) newView);
            }
        });
    }
    
    /**
     * Update the tree display from the given MapView.
     */
    private void updateTreeFromMap(MapView mapView) {
        try {
            // Clean up old tree listeners
            cleanupCurrentTree();
            
            // Create new tree from map
            TreeNode newRoot = NodeTreeFactory.createTreeFromMap(mapView, this);
            
            if (newRoot != null) {
                currentRoot = newRoot;
                currentMapView = mapView;
                setRootNode(newRoot);
            } else {
                showNoMapState();
            }
        } catch (Exception e) {
            System.err.println("Failed to update tree from map: " + e.getMessage());
            e.printStackTrace();
            showNoMapState();
        }
    }
    
    /**
     * Show "No Map Available" state.
     */
    private void showNoMapState() {
        cleanupCurrentTree();
        currentRoot = new TreeNode("No Map Available", "empty");
        currentMapView = null;
        setRootNode(currentRoot);
    }
    
    /**
     * Clean up listeners from the current tree to prevent memory leaks.
     */
    private void cleanupCurrentTree() {
        if (currentRoot != null && currentRoot instanceof MapTreeNode) {
            NodeTreeFactory.cleanupTree(currentRoot);
        }
    }
    
    /**
     * Clean up resources when this pane is being destroyed.
     * Call this to prevent memory leaks.
     */
    public void dispose() {
        // Unregister from map view changes
        Controller.getCurrentController().getMapViewManager().removeMapViewChangeListener(this);
        
        // Clean up current tree
        cleanupCurrentTree();
    }
}