/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2013 Dimitry
 *
 *  This file author is Dimitry
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.view.swing.ui;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.FocusManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;

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
    static {
        migrateSelectionPropertiesFromLegacyMethod();
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

	private static final String SELECTION_ON_MOUSE_OVER = "selection_on_mouse_over";
	private static final String SELECTION_DISABLED = "disabled";
	private static final String SELECTION_ENABLED = "enabled";
	private static boolean mouseWasMoved = false;
	private final MovedMouseEventFilter windowMouseTracker = new MovedMouseEventFilter();
	private final MouseTimerDelegate timerDelegate = new MouseTimerDelegate();

	protected static class TimeDelayedSelection implements ActionListener {
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


	public void createTimer(final MouseEvent e) {
		if(! mouseWasMoved) {
			return;
		}
		if (!isInside(e)) {
			return;
		}
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
	public boolean isActionEnabled() {
		final String selectionBehavior = getSelectionBehavior();
		return !selectionBehavior.equals(SELECTION_DISABLED);
	}

	public boolean shouldSelectOnClick(MouseEvent e) {
		if (isInside(e)) {
			final NodeView nodeView = getRelatedNodeView(e);
			return !nodeView.isSelected() || Controller.getCurrentController().getSelection().size() != 1;
		}
		return false;
	}

	public void extendSelection(final MouseEvent e, boolean scrollNodeTree) {
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

	public void selectSingleNode(MouseEvent e) {
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

	public boolean isRelevant(MouseEvent e) {
		return windowMouseTracker.isRelevant(e);
	}

	public void trackWindowForComponent(Component c) {
		windowMouseTracker.trackWindowForComponent(c);
		timerDelegate.trackWindowForComponent(c);
	}

	private String getSelectionBehavior() {
		ResourceController rc = ResourceController.getResourceController();
		return rc.getProperty(SELECTION_ON_MOUSE_OVER, SELECTION_ENABLED);
	}


	private static void migrateSelectionPropertiesFromLegacyMethod() {
		ResourceController rc = ResourceController.getResourceController();
		
		if (shouldMigrateSelectionMethod(rc)) {
			String selectionMethod = rc.getProperty("selection_method");
			migrateSelectionSettingsFromSelectionMethod(rc, selectionMethod);
		}
	}

	private static boolean shouldMigrateSelectionMethod(ResourceController rc) {
		return rc.isPropertySetByUser("selection_method") && 
			   !rc.isPropertySetByUser(SELECTION_ON_MOUSE_OVER);
	}

	private static void migrateSelectionSettingsFromSelectionMethod(ResourceController rc, String selectionMethod) {
		switch (selectionMethod) {
			case "selection_method_direct":
				rc.setProperty(MouseTimerDelegate.MOUSE_OVER_TIMING, MouseTimerDelegate.TIMING_IMMEDIATE);
				rc.setProperty(SELECTION_ON_MOUSE_OVER, SELECTION_ENABLED);
				break;
			case "selection_method_delayed":
				rc.setProperty(MouseTimerDelegate.MOUSE_OVER_TIMING, MouseTimerDelegate.TIMING_DELAYED);
				rc.setProperty(SELECTION_ON_MOUSE_OVER, SELECTION_ENABLED);
				break;
			case "selection_method_by_click":
				rc.setProperty(SELECTION_ON_MOUSE_OVER, SELECTION_DISABLED);
				break;
		}
	}
}