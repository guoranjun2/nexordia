package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Point;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.features.map.NodeModel.Side;
import org.freeplane.features.nodelocation.LocationModel;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.NodeView;

final class FreeNodeCentering {
	static Point centerMapPoint(NodeView nodeView) {
		final MainView mainView = nodeView.getMainView();
		final Point center = new Point(mainView.getWidth() / 2, mainView.getHeight() / 2);
		UITools.convertPointToAncestor(mainView, center, nodeView.getMap());
		return center;
	}

	static FreeNodePlacement.Location locationForCenter(NodeView nodeView, Point requestedCenter) {
		final LocationModel location = LocationModel.getModel(nodeView.getNode());
		return locationForCenter(location.getHGap(), location.getShiftY(), centerMapPoint(nodeView), requestedCenter,
				nodeView.getMap().getZoom(), layoutSide(nodeView),
				nodeView.getAncestorWithVisibleContent().usesHorizontalLayout());
	}

	static FreeNodePlacement.Location locationForCenter(Quantity<LengthUnit> hGap, Quantity<LengthUnit> shiftY,
			Point currentCenter, Point requestedCenter, float zoom, Side layoutSide, boolean usesHorizontalLayout) {
		final int deltaX = Math.round((requestedCenter.x - currentCenter.x) / zoom);
		final int deltaY = Math.round((requestedCenter.y - currentCenter.y) / zoom);
		return FreeNodePlacement.locationForCenter(hGap, shiftY, layoutSide, deltaX, deltaY, usesHorizontalLayout);
	}

	static boolean matches(LocationModel model, FreeNodePlacement.Location location) {
		return model.getHGap().toBaseUnitsRounded() == location.hGap().toBaseUnitsRounded()
				&& model.getShiftY().toBaseUnitsRounded() == location.shiftY().toBaseUnitsRounded();
	}

	static Side layoutSide(NodeView nodeView) {
		return nodeView.isTopOrLeft() ? Side.TOP_OR_LEFT : Side.BOTTOM_OR_RIGHT;
	}

	private FreeNodeCentering() {
	}
}
