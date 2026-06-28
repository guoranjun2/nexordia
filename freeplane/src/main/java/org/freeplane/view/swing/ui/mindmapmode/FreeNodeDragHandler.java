package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.link.ConnectorShape;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.NodeLinkModel;
import org.freeplane.features.map.FreeNode;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.NodeViewFolder;

final class FreeNodeDragHandler extends DragSourceAdapter {
	private final NodeView nodeView;
	private final NodeViewFolder nodeFolder;
	private final NodeModel node;
	private final Quantity<LengthUnit> originalHGap;
	private final Quantity<LengthUnit> originalShiftY;
	private final Point dragStartPoint;
	private final JScrollPane scrollPane;
	private final Side layoutSide;
	private final boolean usesHorizontalLayout;
	private boolean dragActive = true;

	FreeNodeDragHandler(NodeView nodeView, NodeViewFolder nodeFolder, Point dragStartScreenPoint) {
		this.nodeView = nodeView;
		this.nodeFolder = nodeFolder;
		this.node = nodeView.getNode();
		final LocationModel location = LocationModel.getModel(node);
		this.originalHGap = location.getHGap();
		this.originalShiftY = location.getShiftY();
		this.scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, nodeView);
		this.dragStartPoint = dragPoint(dragStartScreenPoint);
		this.layoutSide = FreeNodeCentering.layoutSide(nodeView);
		this.usesHorizontalLayout = nodeView.getAncestorWithVisibleContent().usesHorizontalLayout();
	}

	@Override
	public void dragMouseMoved(DragSourceDragEvent dsde) {
		updateFromDragEvent(dsde);
	}

	@Override
	public void dragOver(DragSourceDragEvent dsde) {
		updateFromDragEvent(dsde);
	}

	private void updateFromDragEvent(DragSourceDragEvent dsde) {
		if (FreeNode.isFreeNode(node)) {
			scheduleMove(dsde.getLocation());
		}
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent dsde) {
		dragActive = false;
		DragSource.getDefaultDragSource().removeDragSourceMotionListener(this);
		nodeFolder.adjustFolding(java.util.Collections.emptySet());
	}

	private FreeNodePlacement.Location locationFor(Point screenPoint) {
		final Point dragNextPoint = dragPoint(screenPoint);
		return FreeNodePlacement.locationForDrag(originalHGap, originalShiftY, dragStartPoint, dragNextPoint,
				nodeView.getMap().getZoom(), layoutSide, usesHorizontalLayout);
	}

	private Point dragPoint(Point screenPoint) {
		final Point point = new Point(screenPoint);
		SwingUtilities.convertPointFromScreen(point, scrollPane);
		findGridPoint(point);
		return point;
	}

	private void findGridPoint(Point point) {
		final int gridSize = ResourceController.getResourceController().getLengthProperty("grid_size");
		if (gridSize <= 2) {
			return;
		}
		point.x -= point.x % gridSize;
		point.y -= point.y % gridSize;
	}

	private void scheduleMove(Point screenPoint) {
		if (screenPoint == null) {
			return;
		}
		if (isOverAnotherNode(screenPoint)) {
			return;
		}
		if (dragActive && FreeNode.isFreeNode(node)) {
			moveDirectly(locationFor(screenPoint));
		}
	}

	private boolean isOverAnotherNode(Point screenPoint) {
		final MapView map = nodeView.getMap();
		final Point mapPoint = new Point(screenPoint);
		SwingUtilities.convertPointFromScreen(mapPoint, map);
		return nodeViewAt(map, mapPoint) != null;
	}

	private NodeView nodeViewAt(Component component, Point point) {
		if (!component.isVisible() || !component.contains(point)) {
			return null;
		}
		if (component instanceof Container) {
			final Container container = (Container) component;
			for (int i = container.getComponentCount() - 1; i >= 0; i--) {
				final Component child = container.getComponent(i);
				final Point childPoint = new Point(point.x - child.getX(), point.y - child.getY());
				final NodeView childNodeView = nodeViewAt(child, childPoint);
				if (childNodeView != null) {
					return childNodeView;
				}
			}
		}
		if (component instanceof MainView) {
			final NodeView candidate = ((MainView) component).getNodeView();
			return candidate.getNode() == node ? null : candidate;
		}
		if (component instanceof NodeView) {
			final NodeView candidate = (NodeView) component;
			return candidate.getNode() == node ? null : candidate;
		}
		return null;
	}

	private void moveDirectly(FreeNodePlacement.Location location) {
		final LocationModel model = LocationModel.createLocationModel(node);
		if (FreeNodeCentering.matches(model, location)) {
			return;
		}
		model.setHGap(location.hGap());
		model.setShiftY(location.shiftY());
		final MapView map = nodeView.getMap();
		map.getModeController().getMapController().nodeRefresh(node);
		nodeView.invalidate();
		map.validate();
		updateConnectors(map);
		map.repaintVisible();
	}

	private void updateConnectors(MapView map) {
		final LinkController linkController = LinkController.getController(map.getModeController());
		final Set<ConnectorModel> updatedConnectors = Collections
				.newSetFromMap(new IdentityHashMap<ConnectorModel, Boolean>());
		updateConnectors(linkController.getLinksFrom(node, map), linkController, updatedConnectors);
		updateConnectors(linkController.getLinksTo(node, map), linkController, updatedConnectors);
	}

	private void updateConnectors(Collection<? extends NodeLinkModel> links, LinkController linkController,
			Set<ConnectorModel> updatedConnectors) {
		for (NodeLinkModel link : links) {
			if (!(link instanceof ConnectorModel)) {
				continue;
			}
			final ConnectorModel connector = (ConnectorModel) link;
			if (!updatedConnectors.add(connector) || ConnectorShape.EDGE_LIKE.equals(linkController.getShape(connector))
					|| connector.isSelfLink()) {
				continue;
			}
			final MapView map = nodeView.getMap();
			final NodeView sourceView = map.getNodeView(connector.getSource());
			final NodeView targetView = map.getNodeView(connector.getTarget());
			if (sourceView == null || targetView == null || !sourceView.isContentVisible()
					|| !targetView.isContentVisible()) {
				continue;
			}
			ConnectorInclination.apply(connector, sourceView, targetView);
		}
	}
}
