package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Dimension;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.features.map.FreeNode;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodelocation.LocationController;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.features.nodelocation.mindmapmode.MLocationController;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

public enum FoldingControlGestureAction {
	SCALE_FREE_NODE_DISTANCE {
		@Override
		Point2D.Double targetLocation(Dimension parentSize, Point2D.Double childShift, Dimension childSize, double direction) {
			return FreeNodeDistanceScaler.scaleChildShiftFromParentCenter(parentSize, childShift, childSize, direction);
		}
	},
	ROTATE_FREE_NODES {
		@Override
		Point2D.Double targetLocation(Dimension parentSize, Point2D.Double childShift, Dimension childSize, double direction) {
			return FreeNodeRotator.rotateChildShiftAroundParentCenter(parentSize, childShift, childSize,
					direction * ROTATION_DEGREES);
		}
	};

	private static final double ROTATION_DEGREES = 5d;

	public static boolean applyDefault(MouseWheelEvent event) {
		if (!(event.getComponent() instanceof MainView)) {
			return false;
		}
		return apply((MainView) event.getComponent(), event.getPoint(), event);
	}

	private static boolean apply(MainView mainView, java.awt.Point point, MouseWheelEvent event) {
		final double direction = -event.getPreciseWheelRotation();
		if (isRotationGesture(event)) {
			return ROTATE_FREE_NODES.consume(mainView, point, direction);
		}
		if (hasModifier(event)) {
			return false;
		}
		return SCALE_FREE_NODE_DISTANCE.consume(mainView, point, direction);
	}

	private boolean consume(MainView mainView, java.awt.Point point, double direction) {
		final MapView map = mainView.getNodeView().getMap();
		if (!map.getModeController().canEdit(map.getMap())) {
			return true;
		}
		if (direction != 0) {
			apply(mainView, direction);
		}
		return true;
	}

	boolean apply(MainView mainView, double direction) {
		final MapView map = mainView.getNodeView().getMap();
		final ModeController modeController = map.getModeController();
		final MLocationController locationController = (MLocationController) LocationController
				.getController(modeController);
		boolean changed = false;
		for (NodeModel child : mainView.getNodeView().getNode().getChildren()) {
			if (!FreeNode.isFreeNode(child)) {
				continue;
			}
			final NodeView childView = map.getNodeView(child);
			if (childView == null) {
				continue;
			}
			final LocationModel location = LocationModel.getModel(child);
			final Point2D.Double targetLocation = targetLocation(unzoomedSize(mainView, map), childShift(location),
					unzoomedSize(childView.getMainView(), map), direction);
			final Quantity<LengthUnit> hGap = new Quantity<LengthUnit>(targetLocation.x, LengthUnit.px);
			final Quantity<LengthUnit> shiftY = new Quantity<LengthUnit>(targetLocation.y, LengthUnit.px);
			if (!hasSameBaseUnits(hGap, location.getHGap()) || !hasSameBaseUnits(shiftY, location.getShiftY())) {
				if (!changed) {
					preserveRootLocation();
				}
				locationController.moveNodePosition(child, hGap, shiftY);
				changed = true;
			}
		}
		return changed;
	}

	abstract Point2D.Double targetLocation(Dimension parentSize, Point2D.Double childShift, Dimension childSize,
			double direction);

	static boolean isRotationGesture(MouseWheelEvent event) {
		return PhysicalKeyboardModifierState.isShiftPressed() && !event.isControlDown() && !event.isAltDown()
				&& !event.isMetaDown();
	}

	static boolean hasModifier(MouseWheelEvent event) {
		return event.isAltDown() || event.isControlDown() || event.isMetaDown()
				|| PhysicalKeyboardModifierState.isShiftPressed();
	}

	private static Point2D.Double childShift(LocationModel location) {
		return new Point2D.Double(location.getHGap().toBaseUnits(), location.getShiftY().toBaseUnits());
	}

	private static Dimension unzoomedSize(MainView view, MapView map) {
		return new Dimension(Math.round(view.getWidth() / map.getZoom()), Math.round(view.getHeight() / map.getZoom()));
	}

	private static boolean hasSameBaseUnits(Quantity<LengthUnit> first, Quantity<LengthUnit> second) {
		return Math.abs(first.toBaseUnits() - second.toBaseUnits()) < 0.0001d;
	}

	private static void preserveRootLocation() {
		final IMapSelection selection = Controller.getCurrentController().getSelection();
		selection.preserveRootNodeLocationOnScreen();
	}
}
