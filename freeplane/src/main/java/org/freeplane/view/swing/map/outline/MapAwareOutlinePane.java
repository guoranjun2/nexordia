
package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.util.List;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import java.awt.Window;

import org.freeplane.features.mode.Controller;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.features.ui.ViewController;
import org.freeplane.view.swing.map.MapView;

import java.util.HashMap;
import java.util.Map;
import org.freeplane.features.ui.FocusOutlineAction;

/** OutlinePane that updates based on map view changes. */
public class MapAwareOutlinePane extends OutlinePane implements IMapViewChangeListener, IMapChangeListener {
	private static final long serialVersionUID = 1L;

	private static final TreeNode NO_MAP_AVAILABLE = new TreeNode("No Map Available", "empty");

    private static final String OUTLINE_STATE_KEY = "freeplane.outline.state";

    private TreeNode currentRoot;
    private MapView currentMapView;

	MapView getCurrentMapView() {
		return currentMapView;
	}

	private final SelectedNodeUpdater selectedNodeUpdater;
    private PropertyChangeListener focusListener;

    private class SelectedNodeUpdater implements INodeSelectionListener{
		@Override
		public void onSelect(NodeModel node) {
			if(currentMapView == null || ! currentMapView.isSelected() || node == null)
				return;
			final ScrollableTreePanel panel = getTreePanel();
			if(panel == null)
				return;
			SwingUtilities.invokeLater(() -> synchronizeOutlineSelection(false));
		}
    }

    void synchronizeOutlineSelection(boolean requestFocus) {
    	final NodeModel node = currentMapView.getSelected().getNode();
    	final ScrollableTreePanel panel = getTreePanel();
        TreeNode target = findVisibleOutlineNodeOrAncestor(node);
        VisibleOutlineState vs = panel.getVisibleState();
        int index = findVisibleIndex(target, vs, panel.getRoot());
        boolean visible = isNodeVisible(target, panel);
        if (!visible) {
		    if (index < 0) index = 0;
		    panel.updateVisibleBlocks(index);
		}
		panel.setSelectedNode(target, requestFocus);
    }

    private TreeNode findVisibleOutlineNodeOrAncestor(NodeModel node) {
    	if(node == null)
    		return null;

    	TreeNode outlineNode = node.getViewers().stream()
    	    	.filter(MapTreeNode.class::isInstance)
    	    	.map(MapTreeNode.class::cast)
    	    	.filter(mapNode -> mapNode.isContainedIn(this))
    	    	.findAny()
    	    	.orElse(null);

    	if(outlineNode == null)
    		return null;
    	return outlineNode.findVisibleAncestorOrSelf();
    }

    private int findVisibleIndex(TreeNode node, VisibleOutlineState vs, TreeNode root) {
        TreeNode n = node;
        while (n != null) {
            int idx = vs.findNodeIndexInVisibleList(n);
            if (idx >= 0) return idx;
            n = n.getParent();
        }
        int rootIdx = vs.findNodeIndexInVisibleList(root);
        return rootIdx >= 0 ? rootIdx : 0;
    }

    private boolean isNodeVisible(TreeNode node, ScrollableTreePanel panel) {
        if (panel.isNodeInBreadcrumbArea(node)) return true;
        for (BlockPanel bp : panel.getVisibleState().getBlockPanels().values()) {
            for (Component comp : bp.getComponents()) {
                if (comp instanceof JButton) {
                    Object n = ((JButton) comp).getClientProperty("treeNode");
                    if (n == node && comp.isShowing()) return true;
                }
            }
        }
        return false;
    }

    public MapAwareOutlinePane() {
    	super(new TreeNode("Loading...", "loading"));
    	selectedNodeUpdater = new SelectedNodeUpdater();
    }



    @Override
    public void addNotify() {
        super.addNotify();
        Controller.getCurrentController().getMapViewManager().addMapViewChangeListener(this);
        try {
            Window w = SwingUtilities.getWindowAncestor(this);
            if (w != null) {
                IMapViewManager mvm = Controller.getCurrentController().getMapViewManager();
                Component mv = mvm.getLastSelectedMapViewContainedIn(w);
                if (mv instanceof MapView) {
                    updateTreeFromMap((MapView) mv);
                } else {
                    showNoMapState();
                }
            }
        } catch (Exception ignore) { /**/}
    }

    @Override
    public void removeNotify() {
    	super.removeNotify();
        Controller.getCurrentController().getMapViewManager().removeMapViewChangeListener(this);
        removeMapListeners();
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

    private void refreshOutlineLater() {
        if (refreshScheduled) {
            return;
        }
        refreshScheduled = true;
        try {
            refreshOutline();
        }
        finally {
            refreshScheduled = false;
        }
    }

    private void refreshOutline() {
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
            removeMapListeners();
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
            } catch (Exception ignore) { /**/}
            NodeTreeBuilder builder = new NodeTreeBuilder(mapView, this, saved).build();

            if (builder.getRoot() != null) {
                currentRoot = builder.getRoot();
                setRootNode(currentRoot);
                ScrollableTreePanel panel = getTreePanel();
                if (panel != null) {
                    panel.setSelectionBridge(new OutlineSelectionBridge(this));
                }
                try {
                    addMapChangeListeners();
                } catch (Exception ignore) { /**/}
                if (builder.getApplicableState() != null) {
                    panel = getTreePanel();
                    if (panel != null) {
                        builder.getApplicableState().applyTo(panel.getRoot());
                        panel.refreshWithBreadcrumbs();
                    }
                }
                if (builder.getFirstVisibleNodeId() != null) {
                    panel = getTreePanel();
                    if (panel != null) {
                        List<FlatNode> nodes = panel.getVisibleState().getVisibleNodes();
                        int index = -1;
                        for (int i = 0; i < nodes.size(); i++) {
                            if (builder.getFirstVisibleNodeId().equals(nodes.get(i).node.getId())) { index = i; break; }
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



	private void addMapChangeListeners() {
		currentMapView.getMap().addMapChangeListener(this);
		currentMapView.getModeController().getMapController().addNodeSelectionListener(selectedNodeUpdater);
        installFocusListener();
	}

    /**
     * Show "No Map Available" state.
     */
    private void showNoMapState() {
        cleanupCurrentTree();
        removeMapListeners();
        currentMapView = null;
        currentRoot = NO_MAP_AVAILABLE;
        setRootNode(currentRoot);
    }



	private void removeMapListeners() {
		if (currentMapView != null) {
            try {
                currentMapView.getMap().removeMapChangeListener(this);
                currentMapView.getModeController().getMapController().removeNodeSelectionListener(selectedNodeUpdater);
                uninstallFocusListener();
            } catch (Exception ignore) { /**/}
        }
	}

    private void installFocusListener() {
        uninstallFocusListener();
        focusListener = ev -> {
            Object nv = ev.getNewValue();
            if (nv == null) return;
            String action = String.valueOf(nv);
            currentMapView.putClientProperty(FocusOutlineAction.OUTLINE_FOCUS_PROPERTY, null);
            if ("back".equals(action)) {
            	currentMapView.getSelected().getMainView().requestFocusInWindow();
                return;
            }
            // Default: switch to outline
            ViewController vc = Controller.getCurrentController().getViewController();
            if (!vc.isOutlineVisible()) {
                vc.setOutlineVisible(true);
            }
            ScrollableTreePanel panel = getTreePanel();
            if (panel != null) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        IMapSelection sel = Controller.getCurrentController().getSelection();
                        NodeModel selectedNode = sel != null ? sel.getSelected() : null;
                        if (selectedNode != null) {
                            synchronizeOutlineSelection(true);
                        }
                        panel.synchronizeSelectionButton(true);

                    } catch (Exception ignore) { /**/}
                });
            }
        };
        try {
            currentMapView.addPropertyChangeListener(FocusOutlineAction.OUTLINE_FOCUS_PROPERTY, focusListener);
        } catch (Exception ignore) { /**/}
    }

    private void uninstallFocusListener() {
        if (currentMapView != null && focusListener != null) {
            try {
                currentMapView.removePropertyChangeListener(FocusOutlineAction.OUTLINE_FOCUS_PROPERTY, focusListener);
            } catch (Exception ignore) { /**/}
        }
        focusListener = null;
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
            refreshOutlineLater();
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
        } catch (Exception ignore) { /**/}
        WeakReference<Filter> ref = new WeakReference<>(currentMapView != null ? currentMapView.getFilter() : null);
        return new OutlineViewState(firstId, levels, rootId, ref);
    }

    private void collectExpanded(TreeNode node, Map<String, Integer> out) {
        int lvl = node.getExpansionLevel();
        if (lvl > 0) out.put(node.getId(), lvl);
        for (TreeNode c : node.getChildren()) collectExpanded(c, out);
    }



}
