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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.freeplane.core.resources.ResourceController;
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
public class NodeFolder implements MouseTimerDelegate.ActionProvider {
    static {
        migratePropertyFromSelectionMethodIfUserCustomized();
    }

    private static final String FOLDING_ON_MOUSE_OVER = "folding_on_mouse_over";
    private static final String FOLDING_DISABLED = "disabled";
    private static final String FOLDING_TOGGLE = "toggle";
    private static final String FOLDING_UNFOLD_ONLY = "unfold_only";
    private static final String FOLDING_PREVIEW = "preview";

    protected class TimeDelayedFolding implements ActionListener {
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

    private final MouseTimerDelegate timerDelegate = new MouseTimerDelegate();
    private NodeModel previewUnfoldedNode = null;
    private TimeDelayedFolding delayedFolding;

    public void createTimer(final MouseEvent e) {
        if(delayedFolding != null && delayedFolding.wasFired) {
            return;
        }
        timerDelegate.createTimer(e, this);
    }

    public void stopTimerForDelayedFolding() {
        timerDelegate.stopTimer();
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

    public void onNodeFoldedByUser(NodeModel node) {
        if (previewUnfoldedNode == node) {
            previewUnfoldedNode = null;
        }
    }

    private void restorePreviewUnfoldedNode() {
        if (previewUnfoldedNode != null && !previewUnfoldedNode.isFolded()) {
            Controller controller = Controller.getCurrentController();
            ModeController modeController = controller.getModeController();
            MapController mapController = modeController.getMapController();
            mapController.setFolded(previewUnfoldedNode, true, controller.getSelection().getFilter());
            previewUnfoldedNode = null;
        }
    }

    @Override
    public ActionListener createDelayedAction(MouseEvent e) {
        final String foldingBehavior = ResourceController.getResourceController().getProperty(FOLDING_ON_MOUSE_OVER);
        delayedFolding = new TimeDelayedFolding(e, foldingBehavior);
        return delayedFolding;
    }

    @Override
    public boolean isActionEnabled() {
        final String foldingBehavior = ResourceController.getResourceController().getProperty(FOLDING_ON_MOUSE_OVER);
        return !foldingBehavior.equals(FOLDING_DISABLED);
    }

    public NodeView getRelatedNodeView(MouseEvent e) {
        return timerDelegate.getRelatedNodeView(e);
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
                rc.setProperty(MouseTimerDelegate.MOUSE_OVER_TIMING, MouseTimerDelegate.TIMING_IMMEDIATE);
                rc.setProperty(FOLDING_ON_MOUSE_OVER, FOLDING_TOGGLE);
                break;
            case "selection_method_delayed":
                rc.setProperty(MouseTimerDelegate.MOUSE_OVER_TIMING, MouseTimerDelegate.TIMING_DELAYED);
                rc.setProperty(FOLDING_ON_MOUSE_OVER, FOLDING_TOGGLE);
                break;
            case "selection_method_by_click":
                rc.setProperty(FOLDING_ON_MOUSE_OVER, FOLDING_DISABLED);
                break;
        }
    }

    public void trackWindowForComponent(Component c) {
        timerDelegate.trackWindowForComponent(c);
    }
}