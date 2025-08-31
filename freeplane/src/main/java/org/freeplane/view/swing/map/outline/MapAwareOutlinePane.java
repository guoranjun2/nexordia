
package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.util.List;
import java.lang.ref.WeakReference;
import javax.swing.SwingUtilities;
import java.awt.Window;

import org.freeplane.features.mode.Controller;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.view.swing.map.MapView;
import java.util.HashMap;
import java.util.Map;

/** OutlinePane that updates based on map view changes. */
public class MapAwareOutlinePane extends OutlinePane implements IMapViewChangeListener, IMapChangeListener {
    private static final TreeNode NO_MAP_AVAILABLE = new TreeNode("No Map Available", "empty");

	private static final String OUTLINE_STATE_KEY = "freeplane.outline.state";

    private TreeNode currentRoot;
    private MapView currentMapView;


    public MapAwareOutlinePane() {
        super(new TreeNode("Loading...", "loading"));
    }



    @Override
    public void addNotify() {
        super.addNotify();
        Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(this);
    }

    @Override
    public void removeNotify() {
        Controller.getCurrentController().getMapViewManager().removeMapViewChangeListener(this);
        if (currentMapView != null) {
            try {
                currentMapView.getMap().removeMapChangeListener(this);
            } catch (Exception ignore) { }
        }
        cleanupCurrentTree();
    }


    @Override
    public void afterFilterChange(Component view, Filter newFilter) {
        if (view instanceof MapView) {
            Window myWindow = SwingUtilities.getWindowAncestor(this);
            if (SwingUtilities.getWindowAncestor(view) == myWindow) {
                updateTreeFromMap((MapView) view);
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
        try {
            refreshOutline(forceRefreshPending);
        }
        finally {
            refreshScheduled = false;
            forceRefreshPending = false;
        }
    }

    private void refreshOutline(boolean force) {
        if (currentMapView == null) {
            showNoMapState();
            return;
        }
        updateTreeFromMap(currentMapView);
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
            ScrollableTreePanel oldPanel = getTreePanel();
            if (oldPanel != null) {
                OutlineViewState captured = captureCurrentState(oldPanel);
                if (currentMapView != null) {
                    currentMapView.putClientProperty(OUTLINE_STATE_KEY, captured);
                }
            }

            cleanupCurrentTree();

            currentMapView = mapView;
            OutlineViewState saved = null;
            try {
                Object cp = currentMapView.getClientProperty(OUTLINE_STATE_KEY);
                if (cp instanceof OutlineViewState) saved = (OutlineViewState) cp;
            } catch (Exception ignore) { }
            NodeTreeBuilder builder = new NodeTreeBuilder(mapView, this, saved).build();

            if (builder.getRoot() != null) {
                currentRoot = builder.getRoot();
                setRootNode(currentRoot);
                try {
                    currentMapView.getMap().addMapChangeListener(this);
                } catch (Exception ignore) { }
                if (builder.getApplicableState() != null) {
                    ScrollableTreePanel panel = getTreePanel();
                    if (panel != null) {
                        builder.getApplicableState().applyTo(panel.getRoot());
                        panel.refreshWithBreadcrumbs();
                    }
                }
                if (builder.getFirstVisibleNodeId() != null) {
                    ScrollableTreePanel panel = getTreePanel();
                    if (panel != null) {
                        List<FlatNode> nodes = panel.getVisibleState().getVisibleNodes();
                        int index = -1;
                        for (int i = 0; i < nodes.size(); i++) {
                            if (builder.getFirstVisibleNodeId().equals(nodes.get(i).node.id)) { index = i; break; }
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
        currentRoot = NO_MAP_AVAILABLE;
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


    @Override
    public void afterWindowLastSelectedMapViewChanged(Window window, Component newView) {
        Window myWindow = SwingUtilities.getWindowAncestor(this);
        if (myWindow == window && newView != currentMapView && newView instanceof MapView) {
		    updateTreeFromMap((MapView) newView);
		}
    }

    @Override
    public void afterWindowLastSelectedMapViewRemoved(Window window) {
        Window myWindow = SwingUtilities.getWindowAncestor(this);
        if (myWindow == window) {
            currentMapView = null;
            showNoMapState();
        }
    }

    /**
     * Clean up listeners from the current tree to prevent memory leaks.
     */
    private void cleanupCurrentTree() {
        if (currentRoot != null && currentRoot instanceof MapTreeNode) {
            OutlinePane.cleanupTree(currentRoot);
        }
    }

    private OutlineViewState captureCurrentState(ScrollableTreePanel panel) {
        String firstId = panel.getVisibleState().getFirstVisibleNodeId();
        TreeNode root = panel.getRoot();
        Map<String, Integer> levels = new HashMap<>();
        collectExpanded(root, levels);
        String rootId = null;
        try {
            if (currentMapView != null && currentMapView.getRoot() != null && currentMapView.getRoot().getNode() != null) {
                rootId = currentMapView.getRoot().getNode().getID();
            }
        } catch (Exception ignore) { }
        WeakReference<Filter> ref = new WeakReference<>(currentMapView != null ? currentMapView.getFilter() : null);
        return new OutlineViewState(firstId, levels, rootId, ref);
    }

    private void collectExpanded(TreeNode node, Map<String, Integer> out) {
        int lvl = node.getExpansionLevel();
        if (lvl > 0) out.put(node.id, lvl);
        for (TreeNode c : node.children) collectExpanded(c, out);
    }



}
