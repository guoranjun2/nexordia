package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class ReadNodeContentTool {
    private final AvailableMaps availableMaps;
    private final NodeContentItemReader nodeContentItemReader;

    public ReadNodeContentTool(AvailableMaps availableMaps, NodeContentItemReader nodeContentItemReader) {
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
        this.nodeContentItemReader = Objects.requireNonNull(nodeContentItemReader, "nodeContentItemReader");
    }

    public ReadNodeContentResponse readNodeContent(ReadNodeContentRequest request) {
        Objects.requireNonNull(request, "request");
        String mapIdentifier = requireValue(request.getMapIdentifier(), "mapIdentifier");
        String nodeIdentifier = requireValue(request.getNodeIdentifier(), "nodeIdentifier");
        UUID mapIdentifierValue = parseMapIdentifier(mapIdentifier);
        MapModel mapModel = availableMaps.findMapModel(mapIdentifierValue);
        if (mapModel == null) {
            throw new IllegalArgumentException("Unknown map identifier: " + mapIdentifier);
        }
        NodeModel focusNode = mapModel.getNodeForID(nodeIdentifier);
        if (focusNode == null) {
            throw new IllegalArgumentException("Unknown node identifier: " + nodeIdentifier);
        }
        NodeContentItem focusNodeItem = nodeContentItemReader.readNodeContentItem(focusNode, NodeContentPreset.FULL);
        NodeContentItem parentNodeItem = nodeContentItemReader.readNodeContentItem(
            focusNode.getParentNode(), NodeContentPreset.BRIEF);
        List<NodeContentItem> childNodes = readChildNodes(focusNode);
        return new ReadNodeContentResponse(mapIdentifier, focusNodeItem, parentNodeItem, childNodes);
    }

    private List<NodeContentItem> readChildNodes(NodeModel focusNode) {
        List<NodeContentItem> childNodes = new ArrayList<>();
        for (NodeModel childNode : focusNode.getChildren()) {
            childNodes.add(nodeContentItemReader.readNodeContentItem(childNode, NodeContentPreset.BRIEF));
        }
        return childNodes;
    }

    private UUID parseMapIdentifier(String mapIdentifier) {
        try {
            return UUID.fromString(mapIdentifier);
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Invalid map identifier: " + mapIdentifier, error);
        }
    }

    private String requireValue(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing " + fieldName);
        }
        return value;
    }
}
