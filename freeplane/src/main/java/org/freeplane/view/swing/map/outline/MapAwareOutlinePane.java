
package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.util.List;
import javax.swing.SwingUtilities;

import org.freeplane.features.mode.Controller;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.view.swing.map.MapView;

/**
 * OutlinePane that automatically updates its content based on map view changes.
 * Implements the same pattern as BookmarkToolbarPane to listen for map switching.
 */
public class MapAwareOutlinePane extends OutlinePane implements IMapViewChangeListener {

    private TreeNode currentRoot;

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
        cleanupCurrentTree();
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

    @Override
    public void afterFilterChange(Component view, Filter newFilter) {
        SwingUtilities.invokeLater(() -> {
            if (view instanceof MapView) {
                updateTreeFromMap((MapView) view);
            }
        });
    }

    /**
     * Update the tree display from the given MapView.
     */
    private void updateTreeFromMap(MapView mapView) {
        try {
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
        currentRoot = new TreeNode("No Map Available", "empty");
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

}
