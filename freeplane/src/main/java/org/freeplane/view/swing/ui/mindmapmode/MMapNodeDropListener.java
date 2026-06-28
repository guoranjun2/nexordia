package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;

import org.freeplane.api.LengthUnit;
import org.freeplane.core.ui.FileOpener;
import org.freeplane.features.map.FreeNode;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.clipboard.MindMapNodesSelection;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.nodelocation.LocationController;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.features.nodelocation.mindmapmode.MLocationController;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.ui.NodeDropUtils;

final class MMapNodeDropListener implements DropTargetListener {
	private final MapView mapView;
	private final FileOpener fileOpener;

	MMapNodeDropListener(MapView mapView, FileOpener fileOpener) {
		this.mapView = mapView;
		this.fileOpener = fileOpener;
	}

	@Override
	public void dragEnter(DropTargetDragEvent dtde) {
		if (acceptsSingleNodeMove(dtde)) {
			dtde.acceptDrag(DnDConstants.ACTION_MOVE);
		}
		else if (isNodeTransfer(dtde.getTransferable())) {
			dtde.rejectDrag();
		}
		else {
			fileOpener.dragEnter(dtde);
		}
	}

	@Override
	public void dragOver(DropTargetDragEvent dtde) {
		if (acceptsSingleNodeMove(dtde)) {
			dtde.acceptDrag(DnDConstants.ACTION_MOVE);
		}
		else if (isNodeTransfer(dtde.getTransferable())) {
			dtde.rejectDrag();
		}
		else {
			fileOpener.dragOver(dtde);
		}
	}

	@Override
	public void dropActionChanged(DropTargetDragEvent dtde) {
		fileOpener.dropActionChanged(dtde);
	}

	@Override
	public void dragExit(DropTargetEvent dte) {
		fileOpener.dragExit(dte);
	}

	@Override
	public void drop(DropTargetDropEvent dtde) {
		try {
			if (!acceptsSingleNodeMove(dtde)) {
				if (isNodeTransfer(dtde.getTransferable())) {
					dtde.rejectDrop();
					return;
				}
				fileOpener.drop(dtde);
				return;
			}
			final NodeModel node = draggedNode(dtde.getTransferable());
			dtde.acceptDrop(DnDConstants.ACTION_MOVE);
			if (!FreeNode.isFreeNode(node)) {
				convertToFreeNode(node, dtde.getLocation());
			}
			dtde.dropComplete(true);
		}
		catch (Exception e) {
			dtde.dropComplete(false);
		}
	}

	private boolean acceptsSingleNodeMove(DropTargetDragEvent event) {
		return acceptsSingleNodeMove(event.getTransferable(),
				NodeDropUtils.getDropAction(event.getTransferable(), event.getDropAction()), event.getLocation());
	}

	private boolean acceptsSingleNodeMove(DropTargetDropEvent event) {
		return event.isLocalTransfer() && acceptsSingleNodeMove(event.getTransferable(),
				NodeDropUtils.getDropAction(event), event.getLocation());
	}

	private boolean acceptsSingleNodeMove(Transferable transferable, int dropAction, Point location) {
		if (dropAction != DnDConstants.ACTION_MOVE
				|| !isNodeTransfer(transferable)) {
			return false;
		}
		try {
			final List<NodeModel> nodes = NodeDropUtils.getNodeObjects(transferable);
			return nodes.size() == 1 && !nodes.get(0).isRoot()
					&& NodeDropUtils.areFromSameMap(mapView.getRoot().getNode(), nodes)
					&& !isOverNode(location, nodes.get(0));
		}
		catch (Exception e) {
			return false;
		}
	}

	private boolean isNodeTransfer(Transferable transferable) {
		return transferable.isDataFlavorSupported(MindMapNodesSelection.mindMapNodeObjectsFlavor);
	}

	private boolean isOverNode(Point location, NodeModel draggedNode) {
		final Component component = SwingUtilities.getDeepestComponentAt(mapView, location.x, location.y);
		final NodeView nodeView = nodeView(component);
		return nodeView != null && (!FreeNode.isFreeNode(draggedNode) || nodeView.getNode() != draggedNode);
	}

	private NodeView nodeView(Component component) {
		if (component instanceof MainView) {
			return ((MainView) component).getNodeView();
		}
		if (component instanceof NodeView) {
			return (NodeView) component;
		}
		return component != null ? (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, component) : null;
	}

	private NodeModel draggedNode(Transferable transferable) throws Exception {
		return NodeDropUtils.getNodeObjects(transferable).get(0);
	}

	private void convertToFreeNode(NodeModel node, Point mapPoint) {
		final NodeView parentView = mapView.getNodeView(node.getParentNode());
		final FreeNodePlacement.Placement placement = placement(parentView, mapPoint);
		final MMapController mapController = (MMapController) mapView.getModeController().getMapController();
		mapController.setSide(Collections.singletonList(node), placement.side());
		final FreeNode freeNode = mapView.getModeController().getExtension(FreeNode.class);
		freeNode.undoableActivateHook(node, freeNode);
		final MLocationController locationController = (MLocationController) LocationController
				.getController(mapView.getModeController());
		locationController.moveNodePosition(node, LengthUnit.pixelsInPt(placement.point().x),
				LengthUnit.pixelsInPt(placement.point().y));
		centerAfterLayout(node, new Point(mapPoint), true);
	}

	private FreeNodePlacement.Placement placement(NodeView parentView, Point mapPoint) {
		final MainView mainView = parentView.getMainView();
		final Point parentLocation = new Point();
		SwingUtilities.convertPoint(mainView, parentLocation, mapView);
		final Dimension parentSize = mainView.getSize();
		return FreeNodePlacement.fromMapPoint(mapPoint, parentLocation, parentSize, mapView.getZoom(),
				parentView.usesHorizontalLayout(), parentView.childrenSides());
	}

	private void centerAfterLayout(final NodeModel node, final Point requestedCenter, final boolean retryIfMissing) {
		SwingUtilities.invokeLater(() -> {
			final NodeView nodeView = mapView.getNodeView(node);
			if (nodeView == null || nodeView.getMainView().getWidth() == 0 || nodeView.getMainView().getHeight() == 0) {
				if (retryIfMissing) {
					centerAfterLayout(node, requestedCenter, false);
				}
				return;
			}
			final FreeNodePlacement.Location location = FreeNodeCentering.locationForCenter(nodeView, requestedCenter);
			if (!FreeNodeCentering.matches(LocationModel.getModel(node), location)) {
				final MLocationController locationController = (MLocationController) LocationController
						.getController(mapView.getModeController());
				locationController.moveNodePosition(node, location.hGap(), location.shiftY());
			}
		});
	}
}
