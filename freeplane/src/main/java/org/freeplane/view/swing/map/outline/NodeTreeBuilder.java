package org.freeplane.view.swing.map.outline;

import java.util.Objects;

import org.freeplane.features.filter.Filter;
import org.freeplane.features.map.NodeModel;
import org.freeplane.view.swing.map.MapView;

class NodeTreeBuilder {
    private final MapView mapView;
    private final OutlinePane pane;
    private final OutlineViewState saved;

    private NodeModel rootModel;
    private Filter filter;
    private String targetAnchorId;
    private String lastVisibleId;

    private TreeNode root;
    private String firstVisibleNodeId;
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
        this.targetAnchorId = saved != null ? saved.firstVisibleNodeId : null;

        boolean canApply = false;
        if (saved != null) {
            String currentRootId = rootModel.getID();
            org.freeplane.features.filter.Filter savedFilter = saved.savedFilter != null ? saved.savedFilter.get() : null;
            canApply = Objects.equals(saved.rootNodeId, currentRootId) && Objects.equals(savedFilter, filter);
        }
        this.applicableState = canApply ? saved : null;

        this.lastVisibleId = null;
        this.firstVisibleNodeId = null;

        MapTreeNode outRoot = new MapTreeNode(rootModel, pane);
        rootModel.addViewer(outRoot);
        this.root = outRoot;

        boolean rootVisible = (filter == null) || filter.isVisibleOrAncestor(rootModel);
        if (rootVisible) {
            lastVisibleId = rootModel.getID();
        }
        if (targetAnchorId != null && Objects.equals(rootModel.getID(), targetAnchorId)) {
            firstVisibleNodeId = rootVisible ? targetAnchorId : lastVisibleId;
        }

        visitChildren(rootModel, outRoot);

        if (this.applicableState == null && this.root != null) {
            this.root.applyExpansionLevel(1);
        }

        return this;
    }

    private void visitChildren(NodeModel model, MapTreeNode parentOut) {
        for (NodeModel child : model.getChildren()) {
            boolean visible = filter == null || filter.isVisibleOrAncestor(child);
            if (visible) {
                lastVisibleId = child.getID();
            }
            if (targetAnchorId != null && Objects.equals(child.getID(), targetAnchorId)) {
                firstVisibleNodeId = visible ? targetAnchorId : lastVisibleId;
            }
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
    String getFirstVisibleNodeId() { return firstVisibleNodeId; }
    OutlineViewState getApplicableState() { return applicableState; }
}
