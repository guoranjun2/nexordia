/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2013 Dimitry
 *
 *  This file author is Dimitry
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General License for more details.
 *
 *  You should have received a copy of the GNU General License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.ui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.FocusManager;
import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.Compat;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

/**
 * @author Dimitry Polivaev
 * 19.06.2013
 */
public class NodeSelector implements MouseTimerDelegate.ActionProvider {
    private static final String SELECTION_INSIDE_SAME_MAP = "selection_inside.same_map";
	private static final String SELECTION_INSIDE_SELECTED_WINDOW = "selection_inside.selected_window";
	private static final String SELECTION_INSIDE_ANY_MAP = "selection_inside.any_map";
	private static final String SELECTION_INSIDE_SELECTED_MAP_VIEW = "selection_inside.selected_map_view";
	private static final String MOUSE_OVER_SELECTION_INSIDE = "mouse_over_selection_inside";

	static {
        migrateSelectionPropertiesFromLegacyMethod();
    }
	private static void migrateSelectionPropertiesFromLegacyMethod() {
		ResourceController rc = ResourceController.getResourceController();

		final boolean shouldMigrateSelectionMethod = rc.isPropertySetByUser("selection_method") &&
		   !rc.isPropertySetByUser(MOUSE_OVER_SELECTION_TIMING);
		if (shouldMigrateSelectionMethod) {
			String selectionMethod = rc.getProperty("selection_method");
			migrateSelectionSettingsFromSelectionMethod(rc, selectionMethod);
		}
	}

	private static void migrateSelectionSettingsFromSelectionMethod(ResourceController rc, String selectionMethod) {
		switch (selectionMethod) {
			case "selection_method_direct":
				rc.setProperty(MOUSE_OVER_SELECTION_TIMING, SELECTION_IMMEDIATE);
				break;
			case "selection_method_delayed":
				rc.setProperty(MOUSE_OVER_SELECTION_TIMING, SELECTION_DELAYED);
				break;
			case "selection_method_by_click":
				rc.setProperty(MOUSE_OVER_SELECTION_TIMING, SELECTION_DISABLED);
				break;
		}
	}

    static {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            new AWTEventListener() {
                int lastX = -1;
                int lastY = -1;
                @Override
                public void eventDispatched(AWTEvent event) {
                    if (event instanceof MouseEvent) {
                        MouseEvent mouseEvent = (MouseEvent) event;
                        int x = mouseEvent.getXOnScreen();
                        int y = mouseEvent.getYOnScreen();
                        mouseWasMoved = lastX != x || lastY != y;
                        lastX = x;
                        lastY = y;
                    }
                }
            }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK
        );
    }

	private static final String MOUSE_OVER_SELECTION_TIMING = "mouse_over_selection_timing";
	private static final String SELECTION_DISABLED = "disabled";
	private static final String SELECTION_DELAYED = "delayed";
	private static final String SELECTION_IMMEDIATE = "immediate";
	private static boolean mouseWasMoved = false;
	private final MovedMouseEventFilter windowMouseTracker = new MovedMouseEventFilter();
	private final MouseTimerDelegate timerDelegate = new MouseTimerDelegate();

	static class TimeDelayedSelection implements ActionListener {
		private final MouseEvent mouseEvent;

		TimeDelayedSelection(final MouseEvent e) {
			this.mouseEvent = e;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
		    if (mouseEvent.getModifiers() != 0) {
		        return;
		    }
		    try {
		        Controller controller = Controller.getCurrentController();
		        ModeController modeController = controller.getModeController();
                if (!modeController.isBlocked() && controller.getSelection().size() <= 1) {
		            final NodeView nodeV = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class,
		                    mouseEvent.getComponent());
		            MapView map = nodeV.getMap();
		            if (nodeV.isDisplayable() && nodeV.getNode().hasVisibleContent(map.getFilter())) {
		                map.select();
		                NodeModel node = nodeV.getNode();
		                MouseEventActor.INSTANCE.withMouseEvent( () -> {
                            	MapController mapController = modeController.getMapController();
                            	controller.getSelection().selectAsTheOnlyOneSelected(node);
                            	mapController.scrollNodeTreeAfterSelect(node);
							});
		            }
		        }
		    }
		    catch (NullPointerException e) {
		    }
		}
	}


	void createTimer(final MouseEvent e) {
		if(! mouseWasMoved) {
			return;
		}
		if (!isInside(e)) {
			return;
		}

		final String selectionTiming = getSelectionBehavior(e.getComponent());
		if (selectionTiming.equals(SELECTION_DISABLED)) {
			return;
		}
		if (selectionTiming.equals(SELECTION_IMMEDIATE)) {
			ActionListener action = createDelayedAction(e);
			action.actionPerformed(new ActionEvent(this, 0, ""));
			return;
		}
		// SELECTION_DELAYED case
		timerDelegate.createTimer(e, this);
	}

	protected boolean isInside(final MouseEvent e) {
		return new Rectangle(0, 0, e.getComponent().getWidth(), e.getComponent().getHeight())
		    .contains(e.getPoint());
	}

	public void stopTimerForDelayedSelection() {
		timerDelegate.stopTimer();
	}

	@Override
	public ActionListener createDelayedAction(MouseEvent e) {
		return new TimeDelayedSelection(e);
	}

	@Override
	public boolean isActionEnabled(MouseEvent e) {
		final String selectionBehavior = getSelectionBehavior(e.getComponent());
		return !selectionBehavior.equals(SELECTION_DISABLED);
	}

	boolean shouldSelectOnClick(MouseEvent e) {
		if (isInside(e)) {
			final NodeView nodeView = getRelatedNodeView(e);
			return !nodeView.isSelected() || Controller.getCurrentController().getSelection().size() != 1;
		}
		return false;
	}

	void extendSelection(final MouseEvent e, boolean scrollNodeTree) {
		final Controller controller = Controller.getCurrentController();
		final NodeView nodeView = getRelatedNodeView(e);
		final NodeModel newlySelectedNode = nodeView.getNode();
		final boolean extend = Compat.isMacOsX() ? e.isMetaDown() : e.isControlDown();
		final boolean range = e.isShiftDown();
		final IMapSelection selection = controller.getSelection();
		if (range && !extend) {
			selection.selectContinuous(newlySelectedNode);
		}
		else if (extend && !range) {
			selection.toggleSelected(newlySelectedNode);
		}
		if (extend == range) {
			if (!selection.isSelected(newlySelectedNode)
			        || selection.size() != 1
			        || !(FocusManager.getCurrentManager().getFocusOwner() instanceof MainView)) {
				MouseEventActor.INSTANCE.withMouseEvent( () ->
					selection.selectAsTheOnlyOneSelected(newlySelectedNode));
				e.consume();
			}
			if(! extend && scrollNodeTree && ! newlySelectedNode.isFolded()) {
				MouseEventActor.INSTANCE.withMouseEvent( () ->
					controller.getModeController().getMapController().scrollNodeTreeAfterSelect(newlySelectedNode));
                e.consume();
            }
		}
	}

	void selectSingleNode(MouseEvent e) {
		final NodeView nodeV = getRelatedNodeView(e);
		final Controller controller = Controller.getCurrentController();
		if (!((MapView) controller.getMapViewManager().getMapViewComponent()).isSelected(nodeV)) {
			MouseEventActor.INSTANCE.withMouseEvent( () ->
				controller.getSelection().selectAsTheOnlyOneSelected(nodeV.getNode()));
		}
	}

	public NodeView getRelatedNodeView(MouseEvent e) {
		return timerDelegate.getRelatedNodeView(e);
	}

	boolean isRelevant(MouseEvent e) {
		return windowMouseTracker.isRelevant(e);
	}

	void trackWindowForComponent(Component c) {
		windowMouseTracker.trackWindowForComponent(c);
		timerDelegate.trackWindowForComponent(c);
	}

	private String getSelectionBehavior(Component c) {
		ResourceController rc = ResourceController.getResourceController();
		String behavior = rc.getProperty(MOUSE_OVER_SELECTION_TIMING, SELECTION_IMMEDIATE);
		if (SELECTION_DISABLED.equals(behavior)) {
			return SELECTION_DISABLED;
		} else if ("enabled".equals(behavior)) {
			behavior = SELECTION_IMMEDIATE;
			rc.setProperty(MOUSE_OVER_SELECTION_TIMING, SELECTION_IMMEDIATE);
		}
		String selectionInside = rc.getProperty(MOUSE_OVER_SELECTION_INSIDE, SELECTION_INSIDE_SELECTED_MAP_VIEW);
		switch (selectionInside) {
		case SELECTION_INSIDE_ANY_MAP:
			return behavior;
		case SELECTION_INSIDE_SELECTED_MAP_VIEW: {
			final MapView map = (MapView) SwingUtilities.getAncestorOfClass(MapView.class, c);
			return map != null && map.isSelected() ? behavior : SELECTION_DISABLED;
		}
		case SELECTION_INSIDE_SAME_MAP: {
			final MapView map = (MapView) SwingUtilities.getAncestorOfClass(MapView.class, c);
			if( map == null)
				return SELECTION_DISABLED;
			final IMapSelection selection = map.getModeController().getController().getSelection();
			return selection != null  && map.getMap() == selection.getMap() ? behavior : SELECTION_DISABLED;
		}
		case SELECTION_INSIDE_SELECTED_WINDOW: {
			final Window windowAncestor = SwingUtilities.getWindowAncestor(c);
			return  windowAncestor != null && windowAncestor.isFocused() ? behavior : SELECTION_DISABLED;
		}
		default:
			return SELECTION_DISABLED;
		}
	}
}