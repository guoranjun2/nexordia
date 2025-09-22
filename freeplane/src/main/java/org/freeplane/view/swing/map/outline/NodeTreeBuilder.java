package org.freeplane.view.swing.map.outline;

import java.util.Objects;

import org.freeplane.features.filter.Filter;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.SummaryNode;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.view.swing.map.MapView;

class NodeTreeBuilder {
    private final MapView mapView;
    private final OutlinePane pane;
    private final OutlineViewState saved;

    private NodeModel rootModel;
    private Filter filter;
    private NodeModel overrideRootModel;
    private Filter overrideFilter;

    private TreeNode root;
    private OutlineViewState applicableState;

    NodeTreeBuilder(MapView mapView, OutlinePane pane, OutlineViewState saved) {
        this.mapView = mapView;
        this.pane = pane;
        this.saved = saved;
    }

    NodeTreeBuilder build() {
        initializeRootModel();
        if (rootModel == null) {
            return this;
        }
        initializeFilter();

        boolean canApply = false;
        if (saved != null) {
            String currentRootId = rootModel.getID();
            Filter savedFilter = saved.getSavedFilter() != null ? saved.getSavedFilter().get() : null;
            canApply = Objects.equals(saved.getRootNodeId(), currentRootId) && Objects.equals(savedFilter, filter);
        }
        this.applicableState = canApply ? saved : null;

        MapTreeNode outRoot = new MapTreeNode(rootModel, pane, mapView.getModeController().getExtension(NodeStyleController.class),
        		mapView.getBackground());
        rootModel.addViewer(outRoot);
        this.root = outRoot;

        visitChildren(rootModel, outRoot);

        return this;
    }

    NodeTreeBuilder withRootModel(NodeModel rootModel) {
        this.overrideRootModel = rootModel;
        return this;
    }

    NodeTreeBuilder withFilter(Filter filter) {
        this.overrideFilter = filter;
        return this;
    }

    private void initializeRootModel() {
        if (overrideRootModel != null) {
            rootModel = overrideRootModel;
            return;
        }
        rootModel = mapView.getRoot().getNode();
    }

    private void initializeFilter() {
        if (overrideFilter != null) {
            filter = overrideFilter;
            return;
        }
        if (mapView != null) {
            filter = mapView.getFilter();
        } else {
            filter = null;
        }
    }

    private void visitChildren(NodeModel model, MapTreeNode parentOut) {
        for (NodeModel child : model.getChildren()) {
            boolean visible = !(SummaryNode.isSummaryNode(child) || SummaryNode.isFirstGroupNode(child)) && (filter == null || filter.isVisibleOrAncestor(child));
            MapTreeNode nextParent = parentOut;
            if (visible) {
                MapTreeNode out = new MapTreeNode(parentOut, child, pane);
                child.addViewer(out);
                nextParent = out;
            }
            visitChildren(child, nextParent);
        }
    }

    TreeNode getRoot() { return root; }
    OutlineViewState getApplicableState() { return applicableState; }
}
