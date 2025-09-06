/*
 * Created on 6 Sept 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.awt.Window;
import java.lang.ref.WeakReference;

import javax.swing.SwingUtilities;

import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

class OutlineSelectionBridge {
    private final WeakReference<MapView> mapViewRef;
    private final WeakReference<Window> outlineWindowRef;

    OutlineSelectionBridge(MapView mapView, Window outlineWindow) {
        this.mapViewRef = new WeakReference<>(mapView);
        this.outlineWindowRef = new WeakReference<>(outlineWindow);
    }

    public void selectMapNodeById(String nodeId) {
        final MapView mv = mapViewRef.get();
        final Window outlineWindow = outlineWindowRef.get();
        if (mv == null || outlineWindow == null) return;
        if (!mv.isSelected()) return;
        Window mapViewWindow = SwingUtilities.getWindowAncestor(mv);
        if (mapViewWindow != outlineWindow) return;

        Controller controller = Controller.getCurrentController();
        NodeModel current = null;
        IMapSelection selection = controller.getSelection();
        if (selection != null) current = selection.getSelected();
        if (current != null && nodeId != null && nodeId.equals(current.getID())) return;

        NodeModel target = mv.getMap().getNodeForID(nodeId);
        if (target == null) return;

        mv.getModeController().getMapController().displayNode(target);
        final NodeView nodeView = mv.getNodeView(target);
        mv.selectAsTheOnlyOneSelected(nodeView, false);
    }
}