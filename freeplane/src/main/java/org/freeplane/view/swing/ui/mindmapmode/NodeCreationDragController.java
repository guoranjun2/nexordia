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
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.freeplane.api.ChildNodesAlignment;
import org.freeplane.api.ChildrenSides;
import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.edge.EdgeController;
import org.freeplane.features.edge.EdgeModel;
import org.freeplane.features.edge.EdgeStyle;
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
	private static final double FREE_NODE_PORT_SWITCH_RATIO = 1.15d;
	private static final double BEZIER_HANDLE_RATIO = 0.32d;
	private static final int BEZIER_MIN_HANDLE = 24;

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
		private Point currentMapPoint;
		private NodeView targetNodeView;
		private ConnectorLocation startConnectorLocation = ConnectorLocation.RIGHT;
		private ConnectorLocation endConnectorLocation = ConnectorLocation.LEFT;
		private boolean dragged;

		ActiveDrag(MainView sourceMainView, MouseEvent event) {
			this.sourceMainView = sourceMainView;
			this.sourceNode = sourceMainView.getNodeView().getNode();
			this.map = sourceMainView.getNodeView().getMap();
			this.pressScreenPoint = event.getLocationOnScreen();
			this.currentMapPoint = mapPoint(event);
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
			final Stroke oldStroke = graphics.getStroke();
			final Color oldColor = graphics.getColor();
			final Object oldAntialiasing = graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0f, PREVIEW_DASH, 0f));
			graphics.setColor(PREVIEW_COLOR);
			final ConnectorShape connectorShape = targetNodeView != null ? previewConnectorShape() : null;
			if (connectorShape != null && !ConnectorShape.EDGE_LIKE.equals(connectorShape)) {
				paintConnectorPreview(graphics, connectorShape);
			}
			else {
				paintShape(graphics, previewEdge());
			}
			paintTarget(graphics);
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldAntialiasing);
			graphics.setStroke(oldStroke);
			graphics.setColor(oldColor);
		}

		private PreviewEdge previewEdge() {
			if (EdgeStyle.EDGESTYLE_HORIZONTAL.equals(edgeStyle())) {
				return horizontalPreviewEdge();
			}
			return dynamicPreviewEdge();
		}

		private PreviewEdge dynamicPreviewEdge() {
			final Point sourceCenter = mainViewCenterMapPoint(sourceMainView);
			final Point targetReference = targetNodeView != null ? mainViewCenterMapPoint(targetNodeView.getMainView())
					: currentMapPoint;
			ConnectorLocation[] locations = chooseConnectorLocations(targetReference.x - sourceCenter.x,
					targetReference.y - sourceCenter.y, startConnectorLocation);
			Point start = connectorMapPoint(sourceMainView, locations[0]);
			Point end = targetNodeView != null ? connectorMapPoint(targetNodeView.getMainView(), locations[1])
					: currentMapPoint;
			locations = chooseConnectorLocations(end.x - start.x, end.y - start.y, locations[0]);
			startConnectorLocation = locations[0];
			endConnectorLocation = locations[1];
			start = connectorMapPoint(sourceMainView, startConnectorLocation);
			end = targetNodeView != null ? connectorMapPoint(targetNodeView.getMainView(), endConnectorLocation)
					: currentMapPoint;
			align(start, end);
			return new PreviewEdge(start, end, startConnectorLocation, endConnectorLocation);
		}

		private PreviewEdge horizontalPreviewEdge() {
			final Point sourceCenter = mainViewCenterMapPoint(sourceMainView);
			final Point targetReference = targetNodeView != null ? mainViewCenterMapPoint(targetNodeView.getMainView())
					: currentMapPoint;
			final ConnectorLocation[] dynamicLocations = chooseConnectorLocations(targetReference.x - sourceCenter.x,
					targetReference.y - sourceCenter.y, startConnectorLocation);
			final boolean targetTopOrLeft = targetIsTopOrLeft();
			startConnectorLocation = horizontalStartLocation(targetTopOrLeft, dynamicLocations[0]);
			endConnectorLocation = horizontalEndLocation(targetTopOrLeft, dynamicLocations[1]);
			final Point start = connectorMapPoint(sourceMainView, startConnectorLocation);
			final Point end = targetNodeView != null ? connectorMapPoint(targetNodeView.getMainView(), endConnectorLocation)
					: currentMapPoint;
			align(start, end);
			return new PreviewEdge(start, end, startConnectorLocation, endConnectorLocation);
		}

		private void paintShape(Graphics2D graphics, PreviewEdge previewEdge) {
			final EdgeStyle style = edgeStyle();
			if (EdgeStyle.EDGESTYLE_LINEAR.equals(style) || EdgeStyle.EDGESTYLE_SHARP_LINEAR.equals(style)) {
				paintLine(graphics, previewEdge);
			}
			else if (EdgeStyle.EDGESTYLE_HORIZONTAL.equals(style)) {
				paintHorizontalPath(graphics, previewEdge);
			}
			else if (EdgeStyle.EDGESTYLE_SUMMARY.equals(style)) {
				paintSummaryPath(graphics, previewEdge);
			}
			else {
				paintCubic(graphics, previewEdge);
			}
		}

		private EdgeStyle edgeStyle() {
			final EdgeStyle edgeStyle = sourceMainView.getNodeView().getEdgeStyle();
			return edgeStyle != null ? edgeStyle : EdgeController.STANDARD_EDGE_STYLE;
		}

		private void paintCubic(Graphics2D graphics, PreviewEdge previewEdge) {
			final Point start = previewEdge.start;
			final Point end = previewEdge.end;
			final double dx = end.x - start.x;
			final double dy = end.y - start.y;
			final boolean horizontal = isHorizontalConnectorLocation(previewEdge.startLocation);
			final double handle = calcHandle(Math.abs(horizontal ? dx : dy));
			final double sign = (horizontal ? dx : dy) >= 0 ? 1d : -1d;
			final double c1x = horizontal ? start.x + sign * handle : start.x;
			final double c1y = horizontal ? start.y : start.y + sign * handle;
			final double c2x = horizontal ? end.x - sign * handle : end.x;
			final double c2y = horizontal ? end.y : end.y - sign * handle;
			graphics.draw(new CubicCurve2D.Double(start.x, start.y, c1x, c1y, c2x, c2y, end.x, end.y));
		}

		private void paintLine(Graphics2D graphics, PreviewEdge previewEdge) {
			graphics.draw(new Line2D.Double(previewEdge.start, previewEdge.end));
		}

		private void paintHorizontalPath(Graphics2D graphics, PreviewEdge previewEdge) {
			final Point start = previewEdge.start;
			final Point end = previewEdge.end;
			final Path2D path = new Path2D.Double();
			path.moveTo(start.x, start.y);
			final NodeView source = sourceMainView.getNodeView();
			final boolean usesHorizontalLayout = source.usesHorizontalLayout();
			final boolean areChildrenApart = source.getChildNodesAlignment().isStacked();
			int middleGap = map.getZoomed(LocationModel.DEFAULT_HGAP_PX) / 2;
			final boolean left = targetIsTopOrLeft()
					|| !MainView.USE_COMMON_OUT_POINT_FOR_ROOT_NODE && source.isRoot()
							&& (!usesHorizontalLayout && start.x > end.x || usesHorizontalLayout && start.y > end.y);
			if (left) {
				middleGap = -middleGap;
			}
			if (usesHorizontalLayout) {
				final int middleY = start.y + middleGap;
				path.lineTo(start.x, middleY);
				path.lineTo(end.x, middleY);
			}
			else if (areChildrenApart) {
				path.lineTo(start.x, end.y);
			}
			else {
				final int middleX = start.x + middleGap;
				path.lineTo(middleX, start.y);
				path.lineTo(middleX, end.y);
			}
			path.lineTo(end.x, end.y);
			graphics.draw(path);
		}

		private void paintSummaryPath(Graphics2D graphics, PreviewEdge previewEdge) {
			final Point start = previewEdge.start;
			final Point end = previewEdge.end;
			final boolean isTopOrLeft = targetIsTopOrLeft();
			final int sign = isTopOrLeft ? -1 : 1;
			final int xctrl = map.getZoomed(sign * 4);
			final int childXctrl = map.getZoomed(sign * 20);
			final Path2D path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
			if (sourceMainView.getNodeView().usesHorizontalLayout()) {
				final int startY = isTopOrLeft ? Math.min(start.y, end.y - childXctrl)
						: Math.max(start.y, end.y - childXctrl);
				path.moveTo(start.x, startY);
				path.lineTo(start.x, startY + xctrl);
				path.curveTo(start.x, startY + 2 * xctrl, end.x, startY, end.x, end.y);
			}
			else {
				final int startX = isTopOrLeft ? Math.min(start.x, end.x - childXctrl)
						: Math.max(start.x, end.x - childXctrl);
				path.moveTo(startX, start.y);
				path.lineTo(startX + xctrl, start.y);
				path.curveTo(startX + 2 * xctrl, start.y, startX, end.y, end.x, end.y);
			}
			graphics.draw(path);
		}

		private double calcHandle(double mainDistance) {
			if (mainDistance <= 1) {
				return 0;
			}
			final double handle = mainDistance * BEZIER_HANDLE_RATIO;
			return Math.max(handle, Math.min(BEZIER_MIN_HANDLE, mainDistance * 0.5d));
		}

		private ConnectorShape previewConnectorShape() {
			final ConnectorModel connector = new ConnectorModel(sourceNode, null);
			return LinkController.getController(map.getModeController()).getShape(connector);
		}

		private void paintConnectorPreview(Graphics2D graphics, ConnectorShape shape) {
			final NodeView sourceView = sourceMainView.getNodeView();
			final ConnectorInclination.Inclination inclination = ConnectorInclination.forViews(sourceView, targetNodeView);
			final Point startInclination = inclination.start();
			final Point endInclination = inclination.end();
			final Point start = sourceView.getLinkPoint(startInclination);
			final Point end = targetNodeView.getLinkPoint(endInclination);
			if (ConnectorShape.LINE.equals(shape)) {
				graphics.draw(new Line2D.Double(start, end));
				return;
			}
			final Point startControl = controlPoint(sourceView, start, startInclination);
			final Point endControl = controlPoint(targetNodeView, end, endInclination);
			if (ConnectorShape.LINEAR_PATH.equals(shape)) {
				final Path2D path = new Path2D.Double();
				path.moveTo(start.x, start.y);
				path.lineTo(startControl.x, startControl.y);
				path.lineTo(endControl.x, endControl.y);
				path.lineTo(end.x, end.y);
				graphics.draw(path);
			}
			else {
				graphics.draw(new CubicCurve2D.Double(start.x, start.y, startControl.x, startControl.y,
						endControl.x, endControl.y, end.x, end.y));
			}
		}

		private Point controlPoint(NodeView nodeView, Point linkPoint, Point inclination) {
			final Point point = new Point(linkPoint);
			point.translate((nodeView.isTopOrLeft() ? -1 : 1) * nodeView.getMap().getZoomed(inclination.x),
					nodeView.getMap().getZoomed(inclination.y));
			return point;
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
			if (!ConnectorShape.EDGE_LIKE.equals(linkController.getShape(connector))) {
				ConnectorInclination.apply(connector, sourceMainView.getNodeView(), targetNodeView);
			}
			map.repaintVisible();
		}

		private NodeModel createFreeNode(Point mapPoint) {
			final FreeNodePlacement.Placement placement = placement(mapPoint);
			final MMapController mapController = (MMapController) map.getModeController().getMapController();
			final NodeModel freeNode = addDefaultFreeNode(mapController, placement.point(), placement.side());
			centerAndEditNewFreeNodeLater(freeNode, new Point(mapPoint), true);
			return freeNode;
		}

		private FreeNodePlacement.Placement placement(Point mapPoint) {
			final Point sourceLocation = new Point();
			UITools.convertPointToAncestor(sourceMainView, sourceLocation, map);
			final Dimension sourceSize = sourceMainView.getSize();
			final boolean usesHorizontalLayout = sourceMainView.getNodeView().usesHorizontalLayout();
			final ChildrenSides childrenSides = sourceMainView.getNodeView().childrenSides();
			return FreeNodePlacement.fromMapPoint(mapPoint, sourceLocation, sourceSize, map.getZoom(),
					usesHorizontalLayout, childrenSides);
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
				inheritParentEdgeStyle(node);
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

		private void inheritParentEdgeStyle(NodeModel node) {
			final EdgeStyle edgeStyle = edgeStyle();
			if (!EdgeController.STANDARD_EDGE_STYLE.equals(edgeStyle)) {
				EdgeModel.createEdgeModel(node).setStyle(edgeStyle);
			}
		}

		private void centerAndEditNewFreeNodeLater(NodeModel freeNode, Point requestedCenter, boolean retryIfMissing) {
			if (freeNode == null) {
				return;
			}
			SwingUtilities.invokeLater(() -> {
				final NodeView freeNodeView = map.getNodeView(freeNode);
				if (freeNodeView == null || freeNodeView.getMainView().getWidth() == 0
						|| freeNodeView.getMainView().getHeight() == 0) {
					if (retryIfMissing) {
						centerAndEditNewFreeNodeLater(freeNode, requestedCenter, false);
					}
					else {
						editNewFreeNode(freeNode);
					}
					return;
				}
				centerNewFreeNode(freeNodeView, requestedCenter);
				editNewFreeNode(freeNode);
			});
		}

		private void centerNewFreeNode(NodeView freeNodeView, Point requestedCenter) {
			final NodeModel freeNode = freeNodeView.getNode();
			final FreeNodePlacement.Location location = FreeNodeCentering.locationForCenter(freeNodeView,
					requestedCenter);
			if (!FreeNodeCentering.matches(LocationModel.getModel(freeNode), location)) {
				final MLocationController locationController = (MLocationController) LocationController
						.getController(map.getModeController());
				locationController.moveNodePosition(freeNode, location.hGap(), location.shiftY());
			}
		}

		private void editNewFreeNode(NodeModel freeNode) {
			final TextController textController = TextController.getController();
			if (freeNode != null && textController instanceof MTextController) {
				((MTextController) textController).edit(freeNode, sourceNode, true, false, false);
			}
		}

		private Point mainViewCenterMapPoint(MainView mainView) {
			return SwingUtilities.convertPoint(mainView,
					new Point(mainView.getWidth() / 2, mainView.getHeight() / 2), map);
		}

		private Point connectorMapPoint(MainView mainView, ConnectorLocation location) {
			final Point connectorPoint = connectorPoint(mainView, location);
			return SwingUtilities.convertPoint(mainView, connectorPoint, map);
		}

		private Point connectorPoint(MainView mainView, ConnectorLocation location) {
			switch (location) {
				case LEFT:
					return mainView.getLeftPoint();
				case RIGHT:
					return mainView.getRightPoint();
				case TOP:
					return mainView.getTopPoint();
				case BOTTOM:
					return mainView.getBottomPoint();
				default:
					return mainView.getRightPoint();
			}
		}

		private ConnectorLocation horizontalStartLocation(boolean targetTopOrLeft, ConnectorLocation fallback) {
			final NodeView source = sourceMainView.getNodeView();
			final boolean usesHorizontalLayout = source.usesHorizontalLayout();
			final ChildNodesAlignment childNodesAlignment = source.getChildNodesAlignment();
			if (!usesHorizontalLayout && childNodesAlignment.isStacked()
					&& source.childrenSides() != ChildrenSides.BOTH_SIDES) {
				return targetTopOrLeft ? ConnectorLocation.RIGHT : ConnectorLocation.LEFT;
			}
			if (source.isRoot() && !MainView.USE_COMMON_OUT_POINT_FOR_ROOT_NODE) {
				return fallback;
			}
			if (usesHorizontalLayout) {
				if (childNodesAlignment == ChildNodesAlignment.AFTER_PARENT
						&& source.childrenSides() == ChildrenSides.BOTH_SIDES) {
					return ConnectorLocation.RIGHT;
				}
				if (childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT
						&& source.childrenSides() == ChildrenSides.BOTH_SIDES) {
					return ConnectorLocation.LEFT;
				}
				if (childNodesAlignment.isStacked()) {
					return fallback;
				}
				return targetTopOrLeft ? ConnectorLocation.TOP : ConnectorLocation.BOTTOM;
			}
			if (childNodesAlignment == ChildNodesAlignment.AFTER_PARENT) {
				return ConnectorLocation.BOTTOM;
			}
			if (childNodesAlignment == ChildNodesAlignment.BEFORE_PARENT) {
				return ConnectorLocation.TOP;
			}
			return targetTopOrLeft ? ConnectorLocation.LEFT : ConnectorLocation.RIGHT;
		}

		private ConnectorLocation horizontalEndLocation(boolean targetTopOrLeft, ConnectorLocation fallback) {
			if (targetNodeView == null) {
				return fallback;
			}
			if (sourceMainView.getNodeView().usesHorizontalLayout()) {
				return targetTopOrLeft ? ConnectorLocation.BOTTOM : ConnectorLocation.TOP;
			}
			return targetTopOrLeft ? ConnectorLocation.RIGHT : ConnectorLocation.LEFT;
		}

		private boolean targetIsTopOrLeft() {
			if (targetNodeView != null) {
				return targetNodeView.isTopOrLeft();
			}
			return placement(currentMapPoint).side() == Side.TOP_OR_LEFT;
		}

		private ConnectorLocation[] chooseConnectorLocations(double dx, double dy, ConnectorLocation fallback) {
			final double absDx = Math.abs(dx);
			final double absDy = Math.abs(dy);
			final boolean horizontal;
			if (absDx > absDy * FREE_NODE_PORT_SWITCH_RATIO) {
				horizontal = true;
			}
			else if (absDy > absDx * FREE_NODE_PORT_SWITCH_RATIO) {
				horizontal = false;
			}
			else {
				horizontal = isHorizontalConnectorLocation(fallback);
			}
			if (horizontal) {
				if (dx >= 0) {
					return new ConnectorLocation[] { ConnectorLocation.RIGHT, ConnectorLocation.LEFT };
				}
				return new ConnectorLocation[] { ConnectorLocation.LEFT, ConnectorLocation.RIGHT };
			}
			if (dy >= 0) {
				return new ConnectorLocation[] { ConnectorLocation.BOTTOM, ConnectorLocation.TOP };
			}
			return new ConnectorLocation[] { ConnectorLocation.TOP, ConnectorLocation.BOTTOM };
		}

		private boolean isHorizontalConnectorLocation(ConnectorLocation location) {
			return ConnectorLocation.LEFT.equals(location) || ConnectorLocation.RIGHT.equals(location);
		}

		private void align(Point start, Point end) {
			if (Math.abs(start.y - end.y) == 1) {
				end.y = start.y;
			}
		}

		private static class PreviewEdge {
			private final Point start;
			private final Point end;
			private final ConnectorLocation startLocation;
			private final ConnectorLocation endLocation;

			private PreviewEdge(Point start, Point end, ConnectorLocation startLocation, ConnectorLocation endLocation) {
				this.start = start;
				this.end = end;
				this.startLocation = startLocation;
				this.endLocation = endLocation;
			}
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
