package org.freeplane.view.swing.map.outline;

import java.util.Objects;

import org.freeplane.features.filter.Filter;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryNode;
import org.freeplane.view.swing.map.MapView;

class NodeTreeBuilder {
    private final MapView mapView;
    private final OutlinePane pane;
    private final OutlineViewState saved;

    private NodeModel rootModel;
    private Filter filter;

    private TreeNode root;
    private OutlineViewState applicableState;

    NodeTreeBuilder(MapView mapView, OutlinePane pane, OutlineViewState saved) {
        this.mapView = mapView;
        this.pane = pane;
        this.saved = saved;
    }

    NodeTreeBuilder build() {
        if (mapView == null || mapView.getMap() == null || mapView.getRoot() == null || mapView.getRoot().getNode() == null) {
            return this;
        }
        this.rootModel = mapView.getRoot().getNode();
        this.filter = mapView.getFilter();

        boolean canApply = false;
        if (saved != null) {
            String currentRootId = rootModel.getID();
            Filter savedFilter = saved.getSavedFilter() != null ? saved.getSavedFilter().get() : null;
            canApply = Objects.equals(saved.getRootNodeId(), currentRootId) && Objects.equals(savedFilter, filter);
        }
        this.applicableState = canApply ? saved : null;

        MapTreeNode outRoot = new MapTreeNode(rootModel, pane);
        rootModel.addViewer(outRoot);
        this.root = outRoot;

        visitChildren(rootModel, outRoot);

        if (this.applicableState == null && this.root != null) {
            this.root.applyExpansionLevel(1);
        }

        return this;
    }

    private void visitChildren(NodeModel model, MapTreeNode parentOut) {
        for (NodeModel child : model.getChildren()) {
            boolean visible = !(SummaryNode.isSummaryNode(child) || SummaryNode.isFirstGroupNode(child)) && (filter == null || filter.isVisibleOrAncestor(child));
            MapTreeNode nextParent = parentOut;
            if (visible) {
                MapTreeNode out = new MapTreeNode(child, pane);
                child.addViewer(out);
                parentOut.addChild(out);
                nextParent = out;
            }
            visitChildren(child, nextParent);
        }
    }

    TreeNode getRoot() { return root; }
    OutlineViewState getApplicableState() { return applicableState; }
}
