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

    private final MapAwareOutlinePane outlinePane;

	OutlineSelectionBridge(MapAwareOutlinePane outlinePane) {
		this.outlinePane = outlinePane;
    }

    public void selectMapNodeById(String nodeId) {
        final MapView mv = outlinePane.getCurrentMapView();
        if (mv == null) return;
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

	void synchronizeOutlineSelection() {
		outlinePane.synchronizeOutlineSelection();
	}
}