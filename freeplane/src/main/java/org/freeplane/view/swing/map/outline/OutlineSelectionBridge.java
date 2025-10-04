/*
 * Created on 6 Sept 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

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
        SwingUtilities.invokeLater(this::focusMapNode);
    }

	void synchronizeOutlineSelection(boolean requestFocusInWindow) {
		outlinePane.synchronizeOutlineSelection(requestFocusInWindow);
	}

	private void focusMapNode() {
        final MapView mv = outlinePane.getCurrentMapView();
        if (mv == null) return;
        mv.getSelected().getMainView().requestFocusInWindow();
	}

	public Collection<? extends TreeNode> collectNodesToSelection(TreeNode ancestor) {
		if(! (ancestor instanceof MapTreeNode))
				return Collections.emptyList();
        final MapView mv = outlinePane.getCurrentMapView();
        if (mv == null)
        	return Collections.emptyList();
        final NodeModel selected = mv.getSelected().getNode();
        final NodeModel ancestorNode = ((MapTreeNode) ancestor).getNodeModel();
        if (!selected.isDescendantOf(ancestorNode))
        	return Collections.emptyList();
        final LinkedList<MapTreeNode> nodes = new LinkedList<MapTreeNode>();
        for(NodeModel node = selected; node != ancestorNode; node = node.getParentNode()) {
        	nodes.addFirst(((MapTreeNode) ancestor).createNode(node));
        }
        int level = ancestor.getLevel();
        for(MapTreeNode node : nodes)
        	node.setLevel(++level);
        return nodes;
	}
}