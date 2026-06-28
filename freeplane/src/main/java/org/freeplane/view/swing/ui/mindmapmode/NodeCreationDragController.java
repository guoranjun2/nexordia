package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.freeplane.api.ChildrenSides;
import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.link.ConnectorModel;
import org.freeplane.features.link.ConnectorShape;
import org.freeplane.features.link.LinkController;
import org.freeplane.features.link.mindmapmode.MLinkController;
import org.freeplane.features.map.FreeNode;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.map.mindmapmode.MMapController;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodelocation.LocationController;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.features.nodelocation.mindmapmode.MLocationController;
import org.freeplane.features.styles.mindmapmode.NewNodeStyle;
import org.freeplane.features.text.TextController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MainView.ConnectorLocation;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

class NodeCreationDragController {
	private static final int CLICK_TOLERANCE = 4;
	private static final float[] PREVIEW_DASH = new float[] { 6f, 5f };
	private static final Color PREVIEW_COLOR = new Color(0x4D8DFF);

	private ActiveDrag activeDrag;
	private boolean suppressNextClick;

	boolean isActive() {
		return activeDrag != null;
	}

	boolean mouseClicked(MouseEvent e) {
		if (!suppressNextClick) {
			return false;
		}
		suppressNextClick = false;
		e.consume();
		return true;
	}

	boolean mouseMoved(MouseEvent e) {
		return activeDrag != null;
	}

	boolean mousePressed(MouseEvent e) {
		if (!isPlainLeftButtonPress(e)) {
			return false;
		}
		final MainView mainView = (MainView) e.getSource();
		if (!canEdit(mainView) || !mainView.getFoldingControlBounds().contains(e.getPoint())) {
			return false;
		}
		activeDrag = new ActiveDrag(mainView, e);
		activeDrag.install();
		mainView.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		e.consume();
		return true;
	}

	boolean mouseDragged(MouseEvent e) {
		if (activeDrag == null) {
			return false;
		}
		if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK) {
			if (activeDrag.hasMovedBeyondClick(e)) {
				activeDrag.dragged = true;
			}
			activeDrag.update(e);
		}
		e.consume();
		return true;
	}

	boolean mouseReleased(MouseEvent e, Consumer<MouseEvent> foldingClickAction) {
		if (activeDrag == null) {
			return false;
		}
		try {
			activeDrag.update(e);
			if (activeDrag.dragged) {
				activeDrag.finish();
			}
			else if (activeDrag.sourceNode.hasChildren()) {
				foldingClickAction.accept(e);
			}
			else {
				createChild(activeDrag.sourceNode);
			}
		}
		finally {
			activeDrag.uninstall();
			activeDrag = null;
			suppressNextClick = true;
			((MainView) e.getSource()).setCursor(Cursor.getDefaultCursor());
			e.consume();
		}
		return true;
	}

	void mouseExited(MouseEvent e) {
	}

	private boolean canEdit(MainView mainView) {
		final MapView map = mainView.getNodeView().getMap();
		final ModeController modeController = map.getModeController();
		return modeController.canEdit(map.getMap());
	}

	private boolean isPlainLeftButtonPress(MouseEvent e) {
		return e.getButton() == MouseEvent.BUTTON1
				&& (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK
				&& !e.isAltDown()
				&& !e.isControlDown()
				&& !e.isMetaDown()
				&& !e.isShiftDown();
	}

	private void createChild(NodeModel sourceNode) {
		final MMapController mapController = (MMapController) Controller.getCurrentModeController().getMapController();
		mapController.select(sourceNode);
		mapController.addNewNode(MMapController.NEW_CHILD);
	}

	private static class ActiveDrag implements MapView.OverlayPainter {
		private final MainView sourceMainView;
		private final NodeModel sourceNode;
		private final MapView map;
		private final Point pressScreenPoint;
		private final ConnectorShape previewShape;
		private Point currentMapPoint;
		private NodeView targetNodeView;
		private boolean dragged;

		ActiveDrag(MainView sourceMainView, MouseEvent event) {
			this.sourceMainView = sourceMainView;
			this.sourceNode = sourceMainView.getNodeView().getNode();
			this.map = sourceMainView.getNodeView().getMap();
			this.pressScreenPoint = event.getLocationOnScreen();
			this.currentMapPoint = mapPoint(event);
			this.previewShape = LinkController.getController(map.getModeController()).getStandardConnectorShape();
		}

		void install() {
			map.setOverlayPainter(this);
		}

		void uninstall() {
			map.setOverlayPainter(null);
		}

		boolean hasMovedBeyondClick(MouseEvent event) {
			return pressScreenPoint.distance(event.getLocationOnScreen()) > CLICK_TOLERANCE;
		}

		void update(MouseEvent event) {
			currentMapPoint = mapPoint(event);
			targetNodeView = nodeViewAt(currentMapPoint);
			if (targetNodeView != null && targetNodeView.getNode() == sourceNode) {
				targetNodeView = null;
			}
			map.repaintVisible();
		}

		void finish() {
			if (targetNodeView != null) {
				addConnector(sourceNode, targetNodeView.getNode());
			}
			else {
				createFreeNode(currentMapPoint);
			}
		}

		@Override
		public void paint(Graphics2D graphics) {
			if (!dragged || currentMapPoint == null) {
				return;
			}
			final Point start = sourceNodeCenterMapPoint();
			final Point end = targetNodeView != null ? targetConnectorMapPoint(start, targetNodeView) : currentMapPoint;
			final Stroke oldStroke = graphics.getStroke();
			final Color oldColor = graphics.getColor();
			final Object oldAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, PREVIEW_DASH, 0f));
			graphics.setColor(PREVIEW_COLOR);
			paintShape(graphics, start, end);
			paintTarget(graphics);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
			graphics.setStroke(oldStroke);
			graphics.setColor(oldColor);
		}

		private void paintShape(Graphics2D graphics, Point start, Point end) {
			switch (previewShape) {
				case CUBIC_CURVE:
					paintCubic(graphics, start, end);
					break;
				case LINEAR_PATH:
				case EDGE_LIKE:
					paintLinearPath(graphics, start, end);
					break;
				case LINE:
				default:
					graphics.draw(new Line2D.Double(start, end));
					break;
			}
		}

		private void paintCubic(Graphics2D graphics, Point start, Point end) {
			final int direction = end.x >= start.x ? 1 : -1;
			final int xControlDistance = Math.max(40, Math.abs(end.x - start.x) / 2);
			graphics.draw(new CubicCurve2D.Double(start.x, start.y,
					start.x + direction * xControlDistance, start.y,
					end.x - direction * xControlDistance, end.y,
					end.x, end.y));
		}

		private void paintLinearPath(Graphics2D graphics, Point start, Point end) {
			final Path2D path = new Path2D.Double();
			path.moveTo(start.x, start.y);
			if (Math.abs(end.x - start.x) >= Math.abs(end.y - start.y)) {
				final int middleX = start.x + (end.x - start.x) / 2;
				path.lineTo(middleX, start.y);
				path.lineTo(middleX, end.y);
			}
			else {
				final int middleY = start.y + (end.y - start.y) / 2;
				path.lineTo(start.x, middleY);
				path.lineTo(end.x, middleY);
			}
			path.lineTo(end.x, end.y);
			graphics.draw(path);
		}

		private void paintTarget(Graphics2D graphics) {
			if (targetNodeView == null) {
				return;
			}
			final MainView targetMainView = targetNodeView.getMainView();
			final Rectangle bounds = SwingUtilities.convertRectangle(targetMainView,
					new Rectangle(0, 0, targetMainView.getWidth(), targetMainView.getHeight()), map);
			graphics.drawRoundRect(bounds.x - 5, bounds.y - 5, bounds.width + 10, bounds.height + 10, 12, 12);
		}

		private void addConnector(NodeModel source, NodeModel target) {
			final MLinkController linkController = (MLinkController) LinkController.getController(map.getModeController());
			final ConnectorModel connector = linkController.addConnector(source, target);
			linkController.setShape(connector, Optional.of(previewShape));
			map.repaintVisible();
		}

		private NodeModel createFreeNode(Point mapPoint) {
			final Point sourceLocation = new Point();
			UITools.convertPointToAncestor(sourceMainView, sourceLocation, map);
			final Dimension sourceSize = sourceMainView.getSize();
			final boolean usesHorizontalLayout = sourceMainView.getNodeView().usesHorizontalLayout();
			final ChildrenSides childrenSides = sourceMainView.getNodeView().childrenSides();
			final FreeNodePlacement.Placement placement = FreeNodePlacement.fromMapPoint(mapPoint, sourceLocation,
					sourceSize, map.getZoom(), usesHorizontalLayout, childrenSides);
			final Side side = placement.side();
			final MMapController mapController = (MMapController) map.getModeController().getMapController();
			final NodeModel freeNode = addDefaultFreeNode(mapController, placement.point(), side);
			centerFreeNodeLater(freeNode, new Point(mapPoint), () -> editNewFreeNode(freeNode));
			return freeNode;
		}

		private NodeModel addDefaultFreeNode(MMapController mapController, Point point, Side side) {
			final ModeController modeController = map.getModeController();
			final TextController textController = TextController.getController();
			if (textController instanceof MTextController) {
				((MTextController) textController).stopInlineEditing();
				modeController.forceNewTransaction();
			}
			if (mapController.isFolded(sourceNode)) {
				mapController.unfold(sourceNode, modeController.getController().getSelection().getFilter());
			}
			final NodeModel freeNode = mapController.addNewNode(sourceNode, mapController.findNewNodePosition(sourceNode), node -> {
				node.setSide(side);
				NewNodeStyle.assignStyleToNewNode(node);
				node.addExtension(modeController.getExtension(FreeNode.class));
			});
			if (freeNode == null) {
				return null;
			}
			final Quantity<LengthUnit> x = LengthUnit.pixelsInPt(point.x);
			final Quantity<LengthUnit> y = LengthUnit.pixelsInPt(point.y);
			final MLocationController locationController = (MLocationController) LocationController.getController(modeController);
			locationController.moveNodePosition(freeNode, x, y);
			mapController.select(freeNode);
			return freeNode;
		}

		private void centerFreeNodeLater(NodeModel freeNode, Point requestedCenter, Runnable afterCentering) {
			if (freeNode == null) {
				return;
			}
			centerFreeNodeAfterLayout(freeNode, requestedCenter, true, afterCentering);
		}

		private void centerFreeNodeAfterLayout(NodeModel freeNode, Point requestedCenter, boolean retryIfMissing,
				Runnable afterCentering) {
			SwingUtilities.invokeLater(() -> {
				final NodeView freeNodeView = map.getNodeView(freeNode);
				if (freeNodeView == null || freeNodeView.getMainView().getWidth() == 0
						|| freeNodeView.getMainView().getHeight() == 0) {
					if (retryIfMissing) {
						centerFreeNodeAfterLayout(freeNode, requestedCenter, false, afterCentering);
					}
					else {
						afterCentering.run();
					}
					return;
				}
				centerFreeNode(freeNode, requestedCenter, freeNodeView);
				afterCentering.run();
			});
		}

		private void centerFreeNode(NodeModel freeNode, Point requestedCenter, NodeView freeNodeView) {
			final MainView freeMainView = freeNodeView.getMainView();
			final Point currentCenter = new Point(freeMainView.getWidth() / 2, freeMainView.getHeight() / 2);
			UITools.convertPointToAncestor(freeMainView, currentCenter, map);
			final int deltaX = Math.round((requestedCenter.x - currentCenter.x) / map.getZoom());
			final int deltaY = Math.round((requestedCenter.y - currentCenter.y) / map.getZoom());
			if (deltaX == 0 && deltaY == 0) {
				return;
			}
			final LocationModel location = LocationModel.getModel(freeNode);
			final boolean usesHorizontalLayout = freeNodeView.getAncestorWithVisibleContent().usesHorizontalLayout();
			final Point correction = FreeNodePlacement.centerCorrection(layoutSide(freeNodeView), deltaX, deltaY,
					usesHorizontalLayout);
			final Quantity<LengthUnit> hGap = location.getHGap().add(correction.x, LengthUnit.px);
			final Quantity<LengthUnit> shiftY = location.getShiftY().add(correction.y, LengthUnit.px);
			final MLocationController locationController = (MLocationController) LocationController
					.getController(map.getModeController());
			locationController.moveNodePosition(freeNode, hGap, shiftY);
		}

		private Side layoutSide(NodeView nodeView) {
			return nodeView.isTopOrLeft() ? Side.TOP_OR_LEFT : Side.BOTTOM_OR_RIGHT;
		}

		private void editNewFreeNode(NodeModel freeNode) {
			final TextController textController = TextController.getController();
			if (freeNode != null && textController instanceof MTextController) {
				((MTextController) textController).edit(freeNode, sourceNode, true, false, false);
			}
		}

		private Point sourceNodeCenterMapPoint() {
			final Point point = new Point(sourceMainView.getWidth() / 2, sourceMainView.getHeight() / 2);
			UITools.convertPointToAncestor(sourceMainView, point, map);
			return point;
		}

		private Point targetConnectorMapPoint(Point sourceMapPoint, NodeView targetNodeView) {
			final MainView targetMainView = targetNodeView.getMainView();
			final Point sourcePointInTarget = SwingUtilities.convertPoint(map, sourceMapPoint, targetMainView);
			final ConnectorLocation location = nearestConnectorLocation(targetMainView, sourcePointInTarget);
			final Point connectorPoint = targetMainView.getConnectorPoint(sourcePointInTarget, location);
			return SwingUtilities.convertPoint(targetMainView, connectorPoint, map);
		}

		private ConnectorLocation nearestConnectorLocation(MainView mainView, Point point) {
			int distance = Math.abs(point.x);
			ConnectorLocation location = ConnectorLocation.LEFT;
			final int rightDistance = Math.abs(mainView.getWidth() - point.x);
			if (rightDistance < distance) {
				distance = rightDistance;
				location = ConnectorLocation.RIGHT;
			}
			final int topDistance = Math.abs(point.y);
			if (topDistance < distance) {
				distance = topDistance;
				location = ConnectorLocation.TOP;
			}
			final int bottomDistance = Math.abs(mainView.getHeight() - point.y);
			if (bottomDistance < distance) {
				location = ConnectorLocation.BOTTOM;
			}
			return location;
		}

		private NodeView nodeViewAt(Point mapPoint) {
			final Component component = SwingUtilities.getDeepestComponentAt(map, mapPoint.x, mapPoint.y);
			if (component == null) {
				return null;
			}
			if (component instanceof MainView) {
				return ((MainView) component).getNodeView();
			}
			if (component instanceof NodeView) {
				return (NodeView) component;
			}
			return (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, component);
		}

		private Point mapPoint(MouseEvent event) {
			final Point point = event.getLocationOnScreen();
			SwingUtilities.convertPointFromScreen(point, map);
			return point;
		}
	}
}
