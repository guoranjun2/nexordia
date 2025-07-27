/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2025 Dimitry
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

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.FocusManager;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.JTextComponent;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.features.filter.FilterController;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

/**
 * @author Dimitry Polivaev
 * 19.06.2013
 */
public class NodeFolder {
    static {
        migratePropertyFromSelectionMethodIfUserCustomized();
    }

    private static final String FOLDING_ON_MOUSE_OVER = "folding_on_mouse_over";
    private static final String MOUSE_OVER_TIMING = "mouse_over_timing";
    private static final String MOUSE_OVER_DELAY = "mouse_over_delay";
    private static final String FOLDING_DISABLED = "disabled";
    private static final String FOLDING_TOGGLE = "toggle";
    private static final String FOLDING_UNFOLD_ONLY = "unfold_only";
    private static final String FOLDING_PREVIEW = "preview";
    private static final String TIMING_IMMEDIATE = "immediate";
    private static final String TIMING_DELAYED = "delayed";

    protected static class TimeDelayedFolding implements ActionListener {
        private final MouseEvent mouseEvent;
        private final String foldingBehavior;
        private boolean wasFired;

        TimeDelayedFolding(final MouseEvent e, String foldingBehavior) {
            this.mouseEvent = e;
            this.foldingBehavior = foldingBehavior;
            this.wasFired = false;
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
                        MouseEventActor.INSTANCE.withMouseEvent(() -> {
                            MapController mapController = modeController.getMapController();
                            performFoldingAction(mapController, node, map, foldingBehavior);
                        });
                        this.wasFired = true;
                    }
                }
            }
            catch (NullPointerException e) {
            }
        }

        private void performFoldingAction(MapController mapController, NodeModel node, MapView map, String behavior) {
            switch (behavior) {
                case FOLDING_TOGGLE:
                    mapController.toggleFoldedAndScroll(node);
                    break;
                case FOLDING_UNFOLD_ONLY:
                    unfoldIfFolded(mapController, node, map);
                    break;
                case FOLDING_PREVIEW:
                    unfoldForPreview(mapController, node, map);
                    break;
                default:
                    break;
            }
        }

        private void unfoldIfFolded(MapController mapController, NodeModel node, MapView map) {
            if (node.isFolded()) {
                mapController.unfoldAndScroll(node, map.getFilter());
            }
        }

        private void unfoldForPreview(MapController mapController, NodeModel node, MapView map) {
            if (node.isFolded()) {
                mapController.unfoldAndScroll(node, map.getFilter());
                previewUnfoldedNode = node;
            }
        }
    }

    private Rectangle controlRegionForDelayedFolding;
    private Timer timerForDelayedFolding;
    private TimeDelayedFolding delayedFolding;
    private static NodeModel previewUnfoldedNode = null;

    public void createTimer(final MouseEvent e) {
        if (controlRegionForDelayedFolding != null && controlRegionForDelayedFolding.contains(e.getPoint())) {
            return;
        }

        stopTimerForDelayedFolding();

        Window focusedWindow = FocusManager.getCurrentManager().getFocusedWindow();
        if (focusedWindow == null) {
            return;
        }
        if (KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner() instanceof JTextComponent) {
            return;
        }

        controlRegionForDelayedFolding = getControlRegion(e.getPoint());
        final String foldingBehavior = ResourceController.getResourceController().getProperty(FOLDING_ON_MOUSE_OVER);

        if (foldingBehavior.equals(FOLDING_DISABLED)) {
            return;
        }

        delayedFolding = new TimeDelayedFolding(e, foldingBehavior);
        final String timing = ResourceController.getResourceController().getProperty(MOUSE_OVER_TIMING);

        if (timing.equals(TIMING_IMMEDIATE)) {
            delayedFolding.actionPerformed(new ActionEvent(this, 0, ""));
            return;
        }

        final int mouseOverDelay = ResourceController.getResourceController().getIntProperty(
                MOUSE_OVER_DELAY, 100);
        timerForDelayedFolding = new Timer(mouseOverDelay, delayedFolding);
        timerForDelayedFolding.setRepeats(false);
        timerForDelayedFolding.start();
    }

    public void stopTimerForDelayedFolding() {
        if (timerForDelayedFolding != null) {
            timerForDelayedFolding.stop();
        }
        timerForDelayedFolding = null;
        controlRegionForDelayedFolding = null;
        delayedFolding = null;
    }

    public void onMouseExited() {
        stopTimerForDelayedFolding();
        restorePreviewUnfoldedNode();
    }

    public void makePreviewUnfoldingPermanent() {
        previewUnfoldedNode = null;
    }

    public boolean isPreviewUnfolded(NodeModel node) {
        return previewUnfoldedNode == node;
    }

    private static void restorePreviewUnfoldedNode() {
        if (previewUnfoldedNode != null && !previewUnfoldedNode.isFolded()) {
            Controller controller = Controller.getCurrentController();
            ModeController modeController = controller.getModeController();
            MapController mapController = modeController.getMapController();
            mapController.setFolded(previewUnfoldedNode, true, controller.getSelection().getFilter());
            previewUnfoldedNode = null;
        }
    }

    protected Rectangle getControlRegion(final Point2D p) {
        final int side = 8;
        return new Rectangle((int) (p.getX() - side / 2), (int) (p.getY() - side / 2), side, side);
    }

    public NodeView getRelatedNodeView(MouseEvent e) {
        return (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, e.getComponent());
    }

    private static void migratePropertyFromSelectionMethodIfUserCustomized() {
        ResourceController rc = ResourceController.getResourceController();
        
        if (shouldMigrateFoldingMethodFromSelectionMethod(rc)) {
            String selectionMethod = rc.getProperty("selection_method");
            migrateFoldingSettingsFromSelectionMethod(rc, selectionMethod);
        }
    }

    private static boolean shouldMigrateFoldingMethodFromSelectionMethod(ResourceController rc) {
        return userHasCustomizedSelectionMethod(rc) && userHasNotCustomizedFoldingMethod(rc);
    }

    private static boolean userHasCustomizedSelectionMethod(ResourceController rc) {
        return rc.isPropertySetByUser("selection_method");
    }

    private static boolean userHasNotCustomizedFoldingMethod(ResourceController rc) {
        return !rc.isPropertySetByUser(FOLDING_ON_MOUSE_OVER);
    }

    private static void migrateFoldingSettingsFromSelectionMethod(ResourceController rc, String selectionMethod) {
        switch (selectionMethod) {
            case "selection_method_direct":
                rc.setProperty(MOUSE_OVER_TIMING, TIMING_IMMEDIATE);
                rc.setProperty(FOLDING_ON_MOUSE_OVER, FOLDING_TOGGLE);
                break;
            case "selection_method_delayed":
                rc.setProperty(MOUSE_OVER_TIMING, TIMING_DELAYED);
                rc.setProperty(FOLDING_ON_MOUSE_OVER, FOLDING_TOGGLE);
                break;
            case "selection_method_by_click":
                rc.setProperty(FOLDING_ON_MOUSE_OVER, FOLDING_DISABLED);
                break;
        }
    }

    public void trackWindowForComponent(Component c) {
        // TODO: Implement window tracking if needed for folding behavior
    }
}