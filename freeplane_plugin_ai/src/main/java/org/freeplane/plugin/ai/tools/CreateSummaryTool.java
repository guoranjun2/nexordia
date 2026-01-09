package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class CreateSummaryTool {
    private final AvailableMaps availableMaps;
    private final NodeModelCreator nodeModelCreator;
    private final NodeInserter nodeInserter;
    private final SummaryNodeCreator summaryNodeCreator;
    private final ModifiedNodeSummaryBuilder modifiedNodeSummaryBuilder;
    private final NodeContentApplier nodeContentApplier;

    public CreateSummaryTool(AvailableMaps availableMaps, NodeModelCreator nodeModelCreator, NodeInserter nodeInserter,
                             SummaryNodeCreator summaryNodeCreator,
                             ModifiedNodeSummaryBuilder modifiedNodeSummaryBuilder,
                             NodeContentApplier nodeContentApplier) {
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
        this.nodeModelCreator = Objects.requireNonNull(nodeModelCreator, "nodeModelCreator");
        this.nodeInserter = Objects.requireNonNull(nodeInserter, "nodeInserter");
        this.summaryNodeCreator = Objects.requireNonNull(summaryNodeCreator, "summaryNodeCreator");
        this.modifiedNodeSummaryBuilder = Objects.requireNonNull(modifiedNodeSummaryBuilder, "modifiedNodeSummaryBuilder");
        this.nodeContentApplier = Objects.requireNonNull(nodeContentApplier, "nodeContentApplier");
    }

    public CreateSummaryResponse createSummary(CreateSummaryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Missing request");
        }
        String mapIdentifierValue = requireValue(request.getMapIdentifier(), "mapIdentifier");
        UUID mapIdentifier = parseMapIdentifier(mapIdentifierValue);
        MapModel mapModel = availableMaps.findMapModel(mapIdentifier);
        if (mapModel == null) {
            throw new IllegalArgumentException("Unknown map identifier: " + mapIdentifierValue);
        }
        String userSummary = requireValue(request.getUserSummary(), "userSummary");
        SummaryAnchorPlacement summaryAnchorPlacement = requireValue(request.getSummaryAnchorPlacement(),
            "summaryAnchorPlacement");
        NodeModel firstNode = resolveNode(mapModel, summaryAnchorPlacement.getFirstNodeIdentifier(),
            "firstNodeIdentifier");
        NodeModel lastNode = resolveNode(mapModel, summaryAnchorPlacement.getLastNodeIdentifier(),
            "lastNodeIdentifier");
        List<NodeCreationItem> nodes = requireNodes(request.getNodes());
        NodeModel rootNode = mapModel.getRootNode();
        NodeModel summaryNode = summaryNodeCreator.createSummaryNode(rootNode, firstNode, lastNode);
        List<NodeModel> createdNodes = new ArrayList<>(nodes.size());
        for (NodeCreationItem nodeItem : nodes) {
            NodeModel nodeModel = nodeModelCreator.createNodeModelTree(nodeItem, mapModel);
            nodeContentApplier.apply(nodeModel, nodeItem);
            createdNodes.add(nodeModel);
        }
        nodeInserter.insertNodes(createdNodes, summaryNode, AnchorPlacementMode.LAST_CHILD);
        List<ModifiedNodeSummary> modifiedNodes = new ArrayList<>();
        modifiedNodes.addAll(modifiedNodeSummaryBuilder.buildSummaries(Collections.singletonList(summaryNode), false));
        modifiedNodes.addAll(modifiedNodeSummaryBuilder.buildSummaries(createdNodes, true));
        String summaryNodeIdentifier = summaryNode.createID();
        return new CreateSummaryResponse(mapIdentifierValue, userSummary, modifiedNodes, summaryNodeIdentifier);
    }

    ToolCallSummary buildToolCallSummary(CreateSummaryRequest request, CreateSummaryResponse response) {
        if (request == null) {
            return null;
        }
        int itemCount = request.getNodes() == null ? 0 : request.getNodes().size();
        String summaryText = "createSummary: items=" + itemCount;
        if (request.getUserSummary() != null && !request.getUserSummary().isEmpty()) {
            summaryText = summaryText + ", userSummary=\"" + request.getUserSummary() + "\"";
        }
        return new ToolCallSummary("createSummary", summaryText, false);
    }

    ToolCallSummary buildToolCallErrorSummary(CreateSummaryRequest request, RuntimeException error) {
        String summaryText = "createSummary error: " + error.getMessage();
        return new ToolCallSummary("createSummary", summaryText, true);
    }

    private NodeModel resolveNode(MapModel mapModel, String nodeIdentifier, String fieldName) {
        String value = requireValue(nodeIdentifier, fieldName);
        NodeModel node = mapModel.getNodeForID(value);
        if (node == null) {
            throw new IllegalArgumentException("Invalid " + fieldName + ": " + value);
        }
        return node;
    }

    private List<NodeCreationItem> requireNodes(List<NodeCreationItem> nodes) {
        if (nodes == null || nodes.isEmpty()) {
            throw new IllegalArgumentException("Nodes list must be non-empty.");
        }
        return nodes;
    }

    private <T> T requireValue(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException("Missing " + fieldName + ".");
        }
        return value;
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + fieldName + ".");
        }
        return value;
    }

    private UUID parseMapIdentifier(String mapIdentifier) {
        try {
            return UUID.fromString(mapIdentifier);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid map identifier: " + mapIdentifier);
        }
    }
}
