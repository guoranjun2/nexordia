package org.freeplane.plugin.ai.tools;

import java.util.List;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;

public class NodeModelCreator {
    public NodeModel createNodeModelTree(NodeCreationItem creationItem, MapModel mapModel) {
        if (creationItem == null) {
            throw new IllegalArgumentException("Missing node creation item.");
        }
        if (mapModel == null) {
            throw new IllegalArgumentException("Missing map model.");
        }
        NodeModel nodeModel = new NodeModel("", mapModel);
        List<NodeCreationItem> children = creationItem.getChildren();
        if (children != null) {
            for (NodeCreationItem childItem : children) {
                NodeModel childModel = createNodeModelTree(childItem, mapModel);
                nodeModel.insert(childModel, nodeModel.getChildCount());
            }
        }
        return nodeModel;
    }
}
