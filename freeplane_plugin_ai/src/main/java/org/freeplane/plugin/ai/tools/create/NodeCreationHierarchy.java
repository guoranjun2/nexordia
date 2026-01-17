package org.freeplane.plugin.ai.tools.create;

import java.util.Collections;
import java.util.List;

import org.freeplane.features.map.NodeModel;

public class NodeCreationHierarchy {
    private final List<NodeModel> rootNodes;
    private final List<NodeModel> createdNodes;

    public NodeCreationHierarchy(List<NodeModel> rootNodes, List<NodeModel> createdNodes) {
        this.rootNodes = rootNodes == null ? Collections.emptyList() : rootNodes;
        this.createdNodes = createdNodes == null ? Collections.emptyList() : createdNodes;
    }

    public List<NodeModel> getRootNodes() {
        return rootNodes;
    }

    public List<NodeModel> getCreatedNodes() {
        return createdNodes;
    }
}
