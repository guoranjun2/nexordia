
package org.freeplane.view.swing.map.outline;

import java.awt.Color;
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
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
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
import org.freeplane.view.swing.map.MapView;

public class MapAwareOutlinePane extends OutlinePane implements IMapViewChangeListener, IMapChangeListener {
	private static final long serialVersionUID = 1L;
	private static final Icon BOOKMARK_ICON = IconStoreFactory.ICON_STORE.getUIIcon("node-bookmark.svg").getIcon();
	private static final Icon SYNC_ICON = ResourceController.getResourceController().getIcon("/images/sync.svg?useAccentColor=true");
	private static final Icon JUMPIN_ICON = ResourceController.getResourceController().getIcon("/images/syncJumpIn.svg?useAccentColor=true");
	private static final TreeNode NO_MAP_AVAILABLE = new TreeNode("empty", () -> TextUtils.getText("no_open_map"));

    private static final String OUTLINE_STATE_KEY = "freeplane.outline.state";

    private TreeNode currentRoot;
    private MapView currentMapView;
    private final OutlineTreeViewStates displayState;
    private final BookmarkModeFilterCache bookmarkFilterCache;
    private JToggleButton bookmarkModeToggleButton;
    private JToggleButton syncModeToggleButton;
    private JToggleButton jumpInToggleButton;

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
        SwingUtilities.invokeLater(() -> synchronizeOutlineSelection(SelectionSynchronizationTrigger.MAP, false));
    }

    void synchronizeOutlineSelection(SelectionSynchronizationTrigger synchronizationTrigger, boolean requestFocus) {
    	final NodeModel node = currentMapView.getSelected().getNode();
    	final ScrollableTreePanel panel = getTreePanel();
        TreeNode target = findOutlineNode(node);
        if(target != null)
        	panel.synchronizeOutlineSelection(target, synchronizationTrigger, requestFocus);
    }

	private TreeNode findOutlineNode(NodeModel node) {
    	if(node == null)
    		return null;

    	TreeNode outlineNode = node.getViewers().stream()
    	    	.filter(MapTreeNode.class::isInstance)
    	    	.map(MapTreeNode.class::cast)
    	    	.filter(mapNode -> mapNode.isContainedIn(this))
    	    	.findAny()
    	    	.orElse(null);

    	if(outlineNode == null)
    		return findOutlineNode(node.getParentNode());
    	return outlineNode;
    }

    public MapAwareOutlinePane() {
    	super(OutlineDisplayMode.DEFAULT, NO_MAP_AVAILABLE);
    	selectedNodeUpdater = new OutlineSelectedNodeUpdater(this);
    	displayState = new OutlineTreeViewStates();
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
                    synchronizeOutlineSelection(SelectionSynchronizationTrigger.MAP, false);
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
				synchronizeOutlineSelection(SelectionSynchronizationTrigger.MAP, requestFocus);
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
    	updateTreeFromMap(mapView, true);
    }
    private void updateTreeFromMap(MapView mapView, boolean captureState) {
        try {
            removeMapListeners();
            ScrollableTreePanel oldPanel = getTreePanel();
            if (captureState && oldPanel != null) {
                storeCurrentDisplayState(oldPanel);
            }

            cleanupCurrentTree();

            currentMapView = mapView;
            OutlineTreeViewState saved = loadSavedViewState(currentMapView);
            NodeTreeBuilder builder = new NodeTreeBuilder(mapView, this, saved);
            final OutlineDisplayMode displayMode = displayState.getCurrentMode();
			if (displayMode == OutlineDisplayMode.BOOKMARK) {
                Filter bookmarkFilter = bookmarkFilterCache.prepare(mapView,
                        () -> Filter.createFilter(this::containsBookmark, true, false, false, null));
                MapModel mapModel = mapView.getMap();
                builder.withRootModel(mapModel.getRootNode());
                if (bookmarkFilter != null) {
                    builder.withFilter(bookmarkFilter);
                }
            }
			else if(! displayState.followsJumpIn())
				builder.withRootModel(mapView.getMap().getRootNode());
            builder.build();
            currentRoot = builder.getRoot();

            if(displayMode != OutlineDisplayMode.BOOKMARK) {
            	final int minimalLevel = displayMode.getMinimalOutlineLevel();
            	if (builder.getApplicableState() == null || currentRoot.getExpansionLevel() < minimalLevel) {
            		final int initialLevel = displayMode.getInitialOutlineLevel();
            		currentRoot.applyExpansionLevel(Math.max(initialLevel, minimalLevel));
            	}
            }
            else if(currentRoot.getExpansionLevel() < 10000) {
            	currentRoot.applyExpansionLevel(10000);
            }
    		setRootNode(displayMode, currentRoot);
    		ScrollableTreePanel panel = getTreePanel();
    		panel.setBackgroundColorSupplier(this::getBackgroundColor);
    		panel.setSelectionBridge(new OutlineSelectionBridge(this));
    		try {
    			addMapChangeListeners();
    		} catch (Exception ignore) {}
    		if (builder.getApplicableState() != null) {
    			panel = getTreePanel();
    			builder.getApplicableState().applyTo(panel.getRoot());
    			panel.updateVisibleNodes();
    		}

    		try {
    			synchronizeOutlineSelection(SelectionSynchronizationTrigger.MAP, false);
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
        setRootNode(OutlineDisplayMode.DEFAULT, currentRoot);
        getTreePanel().setBackgroundColorSupplier(null);
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
            ScrollableTreePanel panel = getTreePanel();
            if (panel != null) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        IMapSelection sel = Controller.getCurrentController().getSelection();
                        NodeModel selectedNode = sel != null ? sel.getSelected() : null;
                        if (selectedNode != null) {
                            synchronizeOutlineSelection(SelectionSynchronizationTrigger.MAP, true);
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
		    synchronizeOutlineSelection(SelectionSynchronizationTrigger.MAP, false);
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

    private OutlineTreeViewState captureCurrentState(ScrollableTreePanel panel) {
        String firstId = panel.getVisibleNodes().getFirstVisibleNodeId();
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
        return new OutlineTreeViewState(firstId, levels, rootId, ref);
    }

    private void collectExpanded(TreeNode node, Map<String, Integer> out) {
        int lvl = node.getExpansionLevel();
        if (lvl > 0) out.put(node.getId(), lvl);
        for (TreeNode c : node.getChildren()) collectExpanded(c, out);
    }




	private void configureToolbar(FreeplaneToolBar toolbar) {
		bookmarkModeToggleButton = new JToggleButton(BOOKMARK_ICON);
		TranslatedElementFactory.createTooltip(bookmarkModeToggleButton, "outline.showBookmarks");
        configureModeToggleButton(bookmarkModeToggleButton, OutlineDisplayMode.BOOKMARK);
        toolbar.add(bookmarkModeToggleButton, 0);
		syncModeToggleButton = new JToggleButton(SYNC_ICON);
		TranslatedElementFactory.createTooltip(syncModeToggleButton, "outline.followSelection");
		configureModeToggleButton(syncModeToggleButton, OutlineDisplayMode.MAP_VIEW_SYNC);
        toolbar.add(syncModeToggleButton, 1);
        jumpInToggleButton = new JToggleButton(JUMPIN_ICON);
        TranslatedElementFactory.createTooltip(jumpInToggleButton, "outline.followRoot");
        configureJumpinToggleButton(jumpInToggleButton);
        toolbar.add(jumpInToggleButton, 2);
	}

	private void configureModeToggleButton(JToggleButton toggleButton, OutlineDisplayMode mode) {
        toggleButton.setFocusable(false);
        toggleButton.addActionListener(e -> {
            OutlineDisplayMode targetMode = toggleButton.isSelected()
                    ? mode
                    : OutlineDisplayMode.MAP_VIEW;
            setOutlineDisplayMode(targetMode, displayState.followsJumpIn());
        });
    }
	private void configureJumpinToggleButton(JToggleButton toggleButton) {
        toggleButton.setFocusable(false);
        toggleButton.setSelected(displayState.followsJumpIn());
        toggleButton.addActionListener(e -> setFollowsJumpIn(toggleButton.isSelected()));
    }

	private void setFollowsJumpIn(final boolean followsJumpIn) {
		setOutlineDisplayMode(getDisplayMode(), followsJumpIn);
	}

    public void setOutlineDisplayMode(OutlineDisplayMode displayMode, final boolean followsJumpIn) {
        final OutlineDisplayMode lastMode = displayState.getCurrentMode();
        final boolean lastFollowsJumpIn = displayState.followsJumpIn();
		if (displayMode == null || displayMode == lastMode && followsJumpIn == lastFollowsJumpIn) {
            syncToggleWithMode();
            return;
        }
        displayState.setCurrentMode(displayMode);
        displayState.setFollowsJumpIn(followsJumpIn);
        ScrollableTreePanel panel = getTreePanel();
        if (currentMapView != null) {
        	if (lastMode.baseMode() != displayMode.baseMode() || followsJumpIn != lastFollowsJumpIn) {
        		storeCurrentDisplayState(panel);
        		updateTreeFromMap(currentMapView, false);
        	}
        	else
        		panel.setDisplayMode(displayMode);

        	synchronizeOutlineSelection(SelectionSynchronizationTrigger.MAP, false);
        }
        syncToggleWithMode();
    }

    private void syncToggleWithMode() {
        boolean bookmarkSelected = displayState.getCurrentMode() == OutlineDisplayMode.BOOKMARK;
        if (bookmarkModeToggleButton.isSelected() != bookmarkSelected) {
            bookmarkModeToggleButton.setSelected(bookmarkSelected);
        }
        boolean syncSelected = displayState.getCurrentMode() == OutlineDisplayMode.MAP_VIEW_SYNC;
        if (syncModeToggleButton.isSelected() != syncSelected) {
            syncModeToggleButton.setSelected(syncSelected);
        }
        final boolean followsJumpIn = displayState.followsJumpIn();
		if (jumpInToggleButton.isSelected() != followsJumpIn) {
            bookmarkModeToggleButton.setSelected(followsJumpIn);
        }
    }

    private void storeCurrentDisplayState(ScrollableTreePanel panel) {
        OutlineTreeViewState state = captureCurrentState(panel);
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

    private OutlineTreeViewState loadSavedViewState(MapView mapView) {
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

	private Color getBackgroundColor() {
		if(currentMapView == null)
			return null;
		boolean useColoredOutlineItems = ResourceController.getResourceController().getBooleanProperty("useColoredOutlineItems", false);
		if(useColoredOutlineItems)
			return currentMapView.getBackground();
		else
			return null;
	}

	@Override
	OutlineDisplayMode getDisplayMode() {
		return displayState.getCurrentMode();
	}

}
