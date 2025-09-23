
package org.freeplane.view.swing.map.outline;

import java.awt.Component;
import java.awt.Window;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.swing.FocusManager;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.bookmarks.mindmapmode.MapBookmarks;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.FocusOutlineAction;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.features.ui.IMapViewManager;
import org.freeplane.features.ui.ViewController;
import org.freeplane.view.swing.map.MapView;

public class MapAwareOutlinePane extends OutlinePane implements IMapViewChangeListener, IMapChangeListener {
	private static final long serialVersionUID = 1L;
	private static final Icon BOOKMARK_ICON = IconStoreFactory.ICON_STORE.getUIIcon("node-bookmark.svg").getIcon();
	private static final TreeNode NO_MAP_AVAILABLE = new TreeNode("empty", () -> TextUtils.getText("no_open_map"));

    private static final String OUTLINE_STATE_KEY = "freeplane.outline.state";

    private TreeNode currentRoot;
    private MapView currentMapView;
    private final OutlineDisplayState displayState;
    private final BookmarkModeFilterCache bookmarkFilterCache;
    private JToggleButton bookmarkModeToggleButton;
    private boolean skipNextStateCapture;

	MapView getCurrentMapView() {
		return currentMapView;
	}

	private final OutlineSelectedNodeUpdater selectedNodeUpdater;
    private PropertyChangeListener focusListener;


    void handleMapSelectionChanged(NodeModel node) {
        if(currentMapView == null || ! currentMapView.isSelected() || node == null)
            return;
        final ScrollableTreePanel panel = getTreePanel();
        if(panel == null)
            return;
        SwingUtilities.invokeLater(() -> synchronizeOutlineSelection(false));
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
    		return findVisibleOutlineNodeOrAncestor(node.getParentNode());
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
        if (panel.isNodeInBreadcrumbArea(node))
        	return true;
        if (panel.isNodeFullyVisibleInViewport(node))
        	return true;
        return false;
    }

    public MapAwareOutlinePane() {
    	super(NO_MAP_AVAILABLE);
    	selectedNodeUpdater = new OutlineSelectedNodeUpdater(this);
    	displayState = new OutlineDisplayState();
    	bookmarkFilterCache = new BookmarkModeFilterCache();
    	configureToolbar(toolbar);
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
                    synchronizeOutlineSelection(false);
                } else {
                    showNoMapState();
                }
            }
        } catch (Exception ignore) {}
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
        if (displayState.getCurrentMode() == OutlineDisplayMode.BOOKMARK) {
            return;
        }
        if (view instanceof MapView) {
            Window myWindow = SwingUtilities.getWindowAncestor(this);
            if (SwingUtilities.getWindowAncestor(view) == myWindow) {
		        final Component focusOwner = FocusManager.getCurrentManager().getFocusOwner();
				final boolean requestFocus = focusOwner != null && SwingUtilities.isDescendingFrom(focusOwner, this);
                updateTreeFromMap((MapView) view);
				synchronizeOutlineSelection(requestFocus);
            }
        }
    }

    private boolean refreshScheduled;

    private void refreshOutlineLater() {
        if (refreshScheduled) {
            return;
        }
        refreshScheduled = true;
        SwingUtilities.invokeLater(this::refreshOutline);
    }

    private void refreshOutline() {
        if (currentMapView == null) {
            showNoMapState();
        }
        else {
        	updateTreeFromMap(currentMapView);
        }
        refreshScheduled = false;
    }



    private void updateTreeFromMap(MapView mapView) {
        try {
            removeMapListeners();
            ScrollableTreePanel oldPanel = getTreePanel();
            if (!skipNextStateCapture && oldPanel != null) {
                storeCurrentDisplayState(oldPanel);
            }
            skipNextStateCapture = false;

            cleanupCurrentTree();

            currentMapView = mapView;
            OutlineViewState saved = loadSavedViewState(currentMapView);
            NodeTreeBuilder builder = new NodeTreeBuilder(mapView, this, saved);
            if (displayState.getCurrentMode() == OutlineDisplayMode.BOOKMARK) {
                Filter bookmarkFilter = bookmarkFilterCache.prepare(mapView,
                        () -> Filter.createFilter(this::containsBookmark, true, false, false, null));
                MapModel mapModel = mapView.getMap();
                builder.withRootModel(mapModel.getRootNode());
                if (bookmarkFilter != null) {
                    builder.withFilter(bookmarkFilter);
                }
            }
            builder.build();
			final int minimalLevel = ResourceController.getResourceController().getIntProperty("minimalFoldableOutlineLevel", 1);


    		currentRoot = builder.getRoot();
    		if (builder.getApplicableState() == null || currentRoot.getExpansionLevel() < minimalLevel) {
    			final int initialLevel = ResourceController.getResourceController().getIntProperty("initiallyExpandedOutlineLevel", 1);
                currentRoot.applyExpansionLevel(Math.max(initialLevel, minimalLevel));
            }
    		setRootNode(currentRoot);
    		ScrollableTreePanel panel = getTreePanel();
    		if (panel != null) {
    			panel.setSelectionBridge(new OutlineSelectionBridge(this));
    		}
    		try {
    			addMapChangeListeners();
    		} catch (Exception ignore) {}
    		if (builder.getApplicableState() != null) {
    			panel = getTreePanel();
    			if (panel != null) {
    				builder.getApplicableState().applyTo(panel.getRoot());
    				panel.updateVisibleNodes();
    			}
    		}

    		try {
    			synchronizeOutlineSelection(false);
    		} catch (Exception ignore) {}

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

    private void showNoMapState() {
        cleanupCurrentTree();
        removeMapListeners();
        currentMapView = null;
        currentRoot = NO_MAP_AVAILABLE;
        bookmarkFilterCache.reset();
        displayState.setCurrentMode(OutlineDisplayMode.MAP_VIEW);
        displayState.putViewState(null);
        setRootNode(currentRoot);
    }



	private void removeMapListeners() {
		if (currentMapView != null) {
            try {
                currentMapView.getMap().removeMapChangeListener(this);
                currentMapView.getModeController().getMapController().removeNodeSelectionListener(selectedNodeUpdater);
                uninstallFocusListener();
            } catch (Exception ignore) {}
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

                    } catch (Exception ignore) {}
                });
            }
        };
        try {
            currentMapView.addPropertyChangeListener(FocusOutlineAction.OUTLINE_FOCUS_PROPERTY, focusListener);
        } catch (Exception ignore) {}
    }

    private void uninstallFocusListener() {
        if (currentMapView != null && focusListener != null) {
            try {
                currentMapView.removePropertyChangeListener(FocusOutlineAction.OUTLINE_FOCUS_PROPERTY, focusListener);
            } catch (Exception ignore) {}
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
        if (MapBookmarks.class.equals(property)) {
            if (displayState.getCurrentMode() == OutlineDisplayMode.BOOKMARK) {
                bookmarkFilterCache.refresh(currentMapView);
                refreshOutlineLater();
            }
            return;
        }
        if ((property == IMapViewManager.MapChangeEventProperty.MAP_VIEW_ROOT
                || IMapViewManager.MapChangeEventProperty.MAP_VIEW_ROOT.equals(property))
                && displayState.getCurrentMode() == OutlineDisplayMode.MAP_VIEW) {
            refreshOutlineLater();
        }
    }


    @Override
    public void afterWindowLastSelectedMapViewChanged(Window window, Component newView) {
        Window myWindow = SwingUtilities.getWindowAncestor(this);
        if (myWindow == window && newView != currentMapView && newView instanceof MapView) {
		    updateTreeFromMap((MapView) newView);
		    synchronizeOutlineSelection(false);
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
        } catch (Exception ignore) {}
        Filter stateFilter = displayState.getCurrentMode() == OutlineDisplayMode.BOOKMARK
                ? bookmarkFilterCache.current()
                : currentMapView != null ? currentMapView.getFilter() : null;
        WeakReference<Filter> ref = new WeakReference<>(stateFilter);
        return new OutlineViewState(firstId, levels, rootId, ref);
    }

    private void collectExpanded(TreeNode node, Map<String, Integer> out) {
        int lvl = node.getExpansionLevel();
        if (lvl > 0) out.put(node.getId(), lvl);
        for (TreeNode c : node.getChildren()) collectExpanded(c, out);
    }




	private void configureToolbar(FreeplaneToolBar toolbar) {
		final JToggleButton bookmarkModeToggleButton = new JToggleButton();
        bookmarkModeToggleButton.setIcon(BOOKMARK_ICON);
        configureBookmarkModeToggleButton(bookmarkModeToggleButton);
        toolbar.add(bookmarkModeToggleButton, 0);
	}

	private void configureBookmarkModeToggleButton(JToggleButton toggleButton) {
        bookmarkModeToggleButton = toggleButton;
        bookmarkModeToggleButton.setEnabled(true);
        bookmarkModeToggleButton.setFocusable(false);
        bookmarkModeToggleButton.addActionListener(e -> {
            OutlineDisplayMode targetMode = bookmarkModeToggleButton.isSelected()
                    ? OutlineDisplayMode.BOOKMARK
                    : OutlineDisplayMode.MAP_VIEW;
            setOutlineDisplayMode(targetMode);
        });
        syncToggleWithMode();
    }

    public void setOutlineDisplayMode(OutlineDisplayMode displayMode) {
        if (displayMode == null || displayMode == displayState.getCurrentMode()) {
            syncToggleWithMode();
            return;
        }
        displayState.setCurrentMode(displayMode);
        ScrollableTreePanel panel = getTreePanel();
        if (panel != null) {
            storeCurrentDisplayState(panel);
        }
        if (currentMapView != null) {
            skipNextStateCapture = true;
            applyDisplayMode(currentMapView);
            synchronizeOutlineSelection(false);
        }
        syncToggleWithMode();
    }

    private void syncToggleWithMode() {
        boolean bookmarkSelected = displayState.getCurrentMode() == OutlineDisplayMode.BOOKMARK;
        if (bookmarkModeToggleButton.isSelected() != bookmarkSelected) {
            bookmarkModeToggleButton.setSelected(bookmarkSelected);
        }
    }

    private void applyDisplayMode(MapView mapView) {
        updateTreeFromMap(mapView);
    }

    private void storeCurrentDisplayState(ScrollableTreePanel panel) {
        OutlineViewState state = captureCurrentState(panel);
        if (state != null) {
            displayState.putViewState(state);
            storeDisplayStatesOnMapView();
        }
    }

    private void storeDisplayStatesOnMapView() {
        if (currentMapView == null) {
            return;
        }
        currentMapView.putClientProperty(OUTLINE_STATE_KEY, displayState.copy());
    }

    private OutlineViewState loadSavedViewState(MapView mapView) {
        if (mapView == null) {
            return null;
        }
        try {
            Object property = mapView.getClientProperty(OUTLINE_STATE_KEY);
            displayState.restoreFrom(property);
        } catch (Exception ignore) {}
        syncToggleWithMode();
        return displayState.getViewState();
    }

    private boolean containsBookmark(NodeModel node) {
        if (node == null) {
            return false;
        }
        MapModel mapModel = node.getMap();
        return mapModel != null && MapBookmarks.of(mapModel).contains(node.getID());
    }

}
