package org.freeplane.plugin.ai.tools;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;

public class NodeModelCreator {
    public NodeModel createNodeModel(MapModel mapModel) {
        if (mapModel == null) {
            throw new IllegalArgumentException("Missing map model.");
        }
        NodeModel nodeModel = new NodeModel("", mapModel);
        return nodeModel;
    }
}
