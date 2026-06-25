package org.freeplane.features.bookmarks.mindmapmode;

import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.filter.Filter;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.filter.hidden.NodeVisibility;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.ui.IMapViewManager;

public class NodeNavigator {
	private static final String BOOKMARK_CENTER_SELECTED_NODE = "bookmark_center_selected_node";
	private static final String BOOKMARK_SET_ZOOM_TO_100 = "bookmark_set_zoom_to_100";
	private final NodeModel node;

	public NodeNavigator(NodeModel node) {
		super();
		this.node = node;
	}

	public NodeModel getNode() {
		return node;
	}

	public void open(boolean openAsRoot) {
		final Controller controller = Controller.getCurrentController();
		final IMapViewManager mapViewManager = controller.getMapViewManager();
		final IMapSelection mapSelection = controller.getSelection();
		final NodeModel nodeToSelect;
		if(openAsRoot) {
			final MapBookmarks mapBookmarks = mapSelection.getMap().getExtension(MapBookmarks.class);
			final NodeModel previouslySelectedNode = mapBookmarks.getSelectedNodeForRoot(node);

			if (mapSelection.getSelectionRoot() != node) {
				mapViewManager.setViewRoot(node);
				nodeToSelect = previouslySelectedNode;
			}
			else
				nodeToSelect = node;
		}
		else
			nodeToSelect = node;
		if(openAsRoot || ! NodeVisibility.isHidden(nodeToSelect)){
			if(mapSelection.getSelectionRoot() != nodeToSelect
					&& ! nodeToSelect.isDescendantOf(mapSelection.getSelectionRoot())) {
				mapViewManager.setViewRoot(node.getMap().getRootNode());
			}
			final Filter filter = mapSelection.getFilter();
			if(! nodeToSelect.isVisible(filter)) {
				FilterController.getController(controller).applyNoFiltering(node.getMap());
			}
			controller.getModeController().getMapController().displayNode(nodeToSelect);
			if(nodeToSelect.isRoot()){
				mapSelection.selectRoot();
			}
			else {
				mapSelection.selectAsTheOnlyOneSelected(nodeToSelect);
				mapSelection.scrollNodeTreeToVisible(nodeToSelect, false);
			}
			applyBookmarkViewport(mapViewManager, mapSelection, nodeToSelect);
		}
	}

	private void applyBookmarkViewport(IMapViewManager mapViewManager, IMapSelection mapSelection, NodeModel nodeToSelect) {
		ResourceController resourceController = ResourceController.getResourceController();
		if (resourceController.getBooleanProperty(BOOKMARK_SET_ZOOM_TO_100)) {
			mapViewManager.setZoom(1f);
		}
		if (resourceController.getBooleanProperty(BOOKMARK_CENTER_SELECTED_NODE)) {
			mapSelection.scrollNodeToCenter(nodeToSelect);
		}
		else if (resourceController.getBooleanProperty(BOOKMARK_SET_ZOOM_TO_100)) {
			mapSelection.scrollNodeToVisible(nodeToSelect);
		}
	}

	public void openAsNewView() {
		final Controller controller = Controller.getCurrentController();
		final IMapViewManager mapViewManager = controller.getMapViewManager();
		mapViewManager.newMapView(node.getMap(), controller.getModeController());
		SwingUtilities.invokeLater(() -> open(true));
	}
}
