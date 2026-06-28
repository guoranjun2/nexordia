package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseWheelEvent;

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
				final Point targetLocation = FreeNodeDistanceScaler.scaleChildShiftFromParentCenter(
						unzoomedSize(mainView, map), childShift(location), unzoomedSize(childView.getMainView(), map),
						direction);
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
	};

	public static boolean applyDefault(MouseWheelEvent event) {
		if (!(event.getComponent() instanceof MainView)) {
			return false;
		}
		return SCALE_FREE_NODE_DISTANCE.apply((MainView) event.getComponent(), event.getPoint(),
				-event.getPreciseWheelRotation());
	}

	private boolean apply(MainView mainView, Point point, double direction) {
		if (direction == 0 || !mainView.getFoldingControlBounds().contains(point)) {
			return false;
		}
		final MapView map = mainView.getNodeView().getMap();
		if (!map.getModeController().canEdit(map.getMap())) {
			return false;
		}
		return apply(mainView, direction);
	}

	abstract boolean apply(MainView mainView, double direction);

	private static Point childShift(LocationModel location) {
		return new Point((int) Math.round(location.getHGap().toBaseUnits()),
				(int) Math.round(location.getShiftY().toBaseUnits()));
	}

	private static Dimension unzoomedSize(MainView view, MapView map) {
		return new Dimension(Math.round(view.getWidth() / map.getZoom()), Math.round(view.getHeight() / map.getZoom()));
	}

	private static boolean hasSameBaseUnits(Quantity<LengthUnit> first, Quantity<LengthUnit> second) {
		return Math.round(first.toBaseUnits()) == Math.round(second.toBaseUnits());
	}

	private static void preserveRootLocation() {
		final IMapSelection selection = Controller.getCurrentController().getSelection();
		selection.preserveRootNodeLocationOnScreen();
	}
}
