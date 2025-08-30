package org.freeplane.view.swing.map.outline;

import java.util.ArrayList;
import java.util.List;

import org.freeplane.features.filter.Filter;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.view.swing.map.MapView;

class OutlineTreeUpdater {
    static class Result {
        final TreeNode root;
        final String firstVisibleNodeId;
        Result(TreeNode root, String firstVisibleNodeId) {
            this.root = root;
            this.firstVisibleNodeId = firstVisibleNodeId;
        }
    }

    static Result updateTreeFromMap(MapView mapView, OutlinePane outlinePane, String prevFirstVisibleId) {
        if (mapView == null || mapView.getMap() == null) {
            return new Result(null, null);
        }

        MapModel map = mapView.getMap();
        NodeModel rootNode = mapView.getRoot().getNode();
        if (rootNode == null) {
            return new Result(null, null);
        }

        // Build new outline tree using current filter
        Filter filter = mapView.getFilter();
        TreeNode newRoot = NodeTreeFactory.createTreeFromMap(mapView, outlinePane);

        // Determine best first visible node id using full preorder of node models
        String anchorId = determineAnchorId(rootNode, filter, prevFirstVisibleId);

        return new Result(newRoot, anchorId);
    }

    private static String determineAnchorId(NodeModel root, Filter filter, String prevId) {
        List<NodeModel> order = new ArrayList<>();
        preorder(root, order);

        int idx = 0;
        if (prevId != null) {
            for (int i = 0; i < order.size(); i++) {
                if (prevId.equals(order.get(i).getID())) { idx = i; break; }
            }
        }

        // Prefer previous if visible now
        NodeModel at = order.get(Math.max(0, Math.min(idx, order.size() - 1)));
        if (isVisibleInOutline(filter, at)) return at.getID();

        // Search predecessor, then successor
        for (int i = idx - 1; i >= 0; i--) {
            NodeModel n = order.get(i);
            if (isVisibleInOutline(filter, n)) return n.getID();
        }
        for (int i = idx + 1; i < order.size(); i++) {
            NodeModel n = order.get(i);
            if (isVisibleInOutline(filter, n)) return n.getID();
        }
        return root.getID();
    }

    private static boolean isVisibleInOutline(Filter filter, NodeModel node) {
        return filter != null && filter.isVisibleOrAncestor(node);
    }

    private static void preorder(NodeModel node, List<NodeModel> out) {
        out.add(node);
        for (NodeModel c : node.getChildren()) preorder(c, out);
    }
}
