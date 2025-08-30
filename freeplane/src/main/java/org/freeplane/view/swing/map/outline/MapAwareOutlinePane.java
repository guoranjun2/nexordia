
package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import java.beans.PropertyChangeListener;

import org.freeplane.features.mode.Controller;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.view.swing.map.MapView;

/**
 * OutlinePane that automatically updates its content based on map view changes.
 * Implements the same pattern as BookmarkToolbarPane to listen for map switching.
 */
public class MapAwareOutlinePane extends OutlinePane implements IMapViewChangeListener, IMapChangeListener {

    private TreeNode currentRoot;
    private MapView currentMapView;
    private PropertyChangeListener lastSelectedListener;
    private JRootPane listenedRootPane;

    public MapAwareOutlinePane() {
        super(new TreeNode("Loading...", "loading"));
    }



    @Override
	public void addNotify() {
		 super.addNotify();
		 Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(this);
        // listen for per-window lastSelected map view changes
        try {
            JRootPane rootPane = SwingUtilities.getRootPane(this);
            if (rootPane != null) {
                listenedRootPane = rootPane;
                lastSelectedListener = evt -> refreshOutlineLater(true);
                rootPane.addPropertyChangeListener(IMapViewManager.LAST_SELECTED_MAP_VIEW_PROPERTY, lastSelectedListener);
            }
        } catch (Exception ignore) { }
	}

    @Override
	public void removeNotify() {
        Controller.getCurrentController().getMapViewManager().removeMapViewChangeListener(this);
        if (currentMapView != null) {
            try {
                currentMapView.getMap().removeMapChangeListener(this);
            } catch (Exception ignore) { }
        }
        if (listenedRootPane != null && lastSelectedListener != null) {
            try {
                listenedRootPane.removePropertyChangeListener(IMapViewManager.LAST_SELECTED_MAP_VIEW_PROPERTY, lastSelectedListener);
            } catch (Exception ignore) { }
            listenedRootPane = null;
            lastSelectedListener = null;
        }
        cleanupCurrentTree();
    }




    @Override
    public void afterViewChange(Component oldView, Component newView) {
        refreshOutlineLater(false);
    }

    @Override
    public void afterViewClose(Component oldView) {
        refreshOutlineLater(false);
    }

    @Override
    public void afterViewCreated(Component newView) {
        refreshOutlineLater(false);
    }

    @Override
    public void afterFilterChange(Component view, Filter newFilter) {
        // update only if the filter change is about the map view in this window
        if (view instanceof MapView) {
            MapView windowMapView = resolveMapViewForThisWindow();
            if (view == windowMapView || view == currentMapView) {
                refreshOutlineLater(true);
            }
        }
    }

    private boolean refreshScheduled;
    private boolean forceRefreshPending;

    private void refreshOutlineLater(boolean force) {
        if (force) {
            forceRefreshPending = true;
        }
        if (refreshScheduled) {
            return;
        }
        refreshScheduled = true;
        SwingUtilities.invokeLater(() -> {
            try {
                refreshOutline(forceRefreshPending);
            }
            finally {
                refreshScheduled = false;
                forceRefreshPending = false;
            }
        });
    }

    private void refreshOutline(boolean force) {
        MapView mapView = resolveMapViewForThisWindow();
        if (mapView == null) {
            currentMapView = null;
            showNoMapState();
            return;
        }
        if (!force && mapView == currentMapView) {
            return;
        }
        updateTreeFromMap(mapView);
    }

    private MapView resolveMapViewForThisWindow() {
        final IMapViewManager mapViewManager = Controller.getCurrentController().getMapViewManager();
        final Component root = SwingUtilities.getRoot(this);
        final JComponent lastSelected = mapViewManager.getLastSelectedMapViewContainedIn(root);
        return lastSelected instanceof MapView ? (MapView) lastSelected : null;
    }

    /**
     * Update the tree display from the given MapView.
     */
    private void updateTreeFromMap(MapView mapView) {
        try {
            if (currentMapView != null && currentMapView.getMap() != null) {
                try {
                    currentMapView.getMap().removeMapChangeListener(this);
                } catch (Exception ignore) { }
            }
            currentMapView = mapView;
            String prevFirstId = null;
            ScrollableTreePanel oldPanel = getTreePanel();
            if (oldPanel != null) {
                prevFirstId = oldPanel.getVisibleState().getFirstVisibleNodeId();
            }

            cleanupCurrentTree();

            OutlineTreeUpdater.Result result = OutlineTreeUpdater.updateTreeFromMap(mapView, this, prevFirstId);

            if (result.root != null) {
                currentRoot = result.root;
                setRootNode(result.root);
                try {
                    currentMapView.getMap().addMapChangeListener(this);
                } catch (Exception ignore) { }
                // Scroll to computed firstVisible if present
                if (result.firstVisibleNodeId != null) {
                    ScrollableTreePanel panel = getTreePanel();
                    if (panel != null) {
                        List<FlatNode> nodes = panel.getVisibleState().getVisibleNodes();
                        int index = -1;
                        for (int i = 0; i < nodes.size(); i++) {
                            if (result.firstVisibleNodeId.equals(nodes.get(i).node.id)) { index = i; break; }
                        }
                        if (index >= 0) {
                            panel.updateVisibleBlocks(index);
                        }
                    }
                }
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
        if (currentMapView != null && currentMapView.getMap() != null) {
            try {
                currentMapView.getMap().removeMapChangeListener(this);
            } catch (Exception ignore) { }
        }
        currentMapView = null;
        currentRoot = new TreeNode("No Map Available", "empty");
        setRootNode(currentRoot);
    }

    @Override
    public void mapChanged(MapChangeEvent event) {
        if (currentMapView == null)
            return;
        if (event.getMap() != currentMapView.getMap())
            return;
        final Object property = event.getProperty();
        if (property == IMapViewManager.MapChangeEventProperty.MAP_VIEW_ROOT
                || IMapViewManager.MapChangeEventProperty.MAP_VIEW_ROOT.equals(property)) {
            refreshOutlineLater(true);
        }
    }

    /**
     * Clean up listeners from the current tree to prevent memory leaks.
     */
    private void cleanupCurrentTree() {
        if (currentRoot != null && currentRoot instanceof MapTreeNode) {
            NodeTreeFactory.cleanupTree(currentRoot);
        }
    }

}
