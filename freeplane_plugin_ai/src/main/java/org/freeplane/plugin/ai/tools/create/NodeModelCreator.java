package org.freeplane.plugin.ai.tools.create;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.edits.AIEdits;

public class NodeModelCreator {
    public NodeModel createNodeModel(MapModel mapModel) {
        if (mapModel == null) {
            throw new IllegalArgumentException("Missing map model.");
        }
        NodeModel nodeModel = new NodeModel("", mapModel);
        nodeModel.addExtension(new AIEdits());
        return nodeModel;
    }
}
