package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;

public class NodeCreationHierarchyBuilder {
    private final NodeModelCreator nodeModelCreator;
    private final NodeContentApplier nodeContentApplier;

    public NodeCreationHierarchyBuilder(NodeModelCreator nodeModelCreator, NodeContentApplier nodeContentApplier) {
        this.nodeModelCreator = Objects.requireNonNull(nodeModelCreator, "nodeModelCreator");
        this.nodeContentApplier = Objects.requireNonNull(nodeContentApplier, "nodeContentApplier");
    }

    public NodeCreationHierarchy buildHierarchy(List<NodeCreationItem> items, MapModel mapModel) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Nodes list must be non-empty.");
        }
        if (mapModel == null) {
            throw new IllegalArgumentException("Missing map model.");
        }
        Map<Integer, NodeModel> nodesByIndex = new HashMap<>();
        Set<Integer> indices = new HashSet<>();
        List<NodeModel> createdNodes = new ArrayList<>(items.size());
        for (NodeCreationItem item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Node item must be non-null.");
            }
            Integer index = item.getIndex();
            if (index == null) {
                throw new IllegalArgumentException("Missing index.");
            }
            if (index < 0) {
                throw new IllegalArgumentException("Invalid index: " + index);
            }
            if (!indices.add(index)) {
                throw new IllegalArgumentException("Duplicate index: " + index);
            }
            NodeModel nodeModel = nodeModelCreator.createNodeModel(mapModel);
            nodesByIndex.put(index, nodeModel);
            createdNodes.add(nodeModel);
            nodeContentApplier.apply(nodeModel, item.getContent());
        }

        List<NodeModel> rootNodes = new ArrayList<>();
        for (NodeCreationItem item : items) {
            Integer parentIndex = item.getParentIndex();
            NodeModel nodeModel = nodesByIndex.get(item.getIndex());
            if (parentIndex == null || parentIndex == -1) {
                rootNodes.add(nodeModel);
                continue;
            }
            if (parentIndex < 0) {
                throw new IllegalArgumentException("Invalid parentIndex: " + parentIndex);
            }
            if (parentIndex.equals(item.getIndex())) {
                throw new IllegalArgumentException("parentIndex must differ from index: " + parentIndex);
            }
            NodeModel parentNode = nodesByIndex.get(parentIndex);
            if (parentNode == null) {
                throw new IllegalArgumentException("Unknown parentIndex: " + parentIndex);
            }
            parentNode.insert(nodeModel, parentNode.getChildCount());
        }

        return new NodeCreationHierarchy(rootNodes, createdNodes);
    }
}
