package org.freeplane.plugin.ai.chat;

import java.util.Objects;
import java.util.UUID;

import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public final class SystemMessageBuilder {
    private static final String NOT_AVAILABLE = "not available";
    private final AvailableMaps availableMaps;

    public SystemMessageBuilder(AvailableMaps availableMaps) {
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
    }

    public String buildForChat() {
        String mapIdentifier = resolveMapIdentifier();
        String rootNodeIdentifier = resolveRootNodeIdentifier();
        String selectedNodeIdentifier = resolveSelectedNodeIdentifier();
        return "Current map identifier: " + mapIdentifier + "\n"
            + "Current root node identifier: " + rootNodeIdentifier + "\n"
            + "Current selected node identifier: " + selectedNodeIdentifier;
    }

    private String resolveMapIdentifier() {
        UUID mapIdentifier = availableMaps.getCurrentMapIdentifier();
        return mapIdentifier == null ? NOT_AVAILABLE : mapIdentifier.toString();
    }

    private String resolveRootNodeIdentifier() {
        MapModel mapModel = availableMaps.getCurrentMapModel();
        if (mapModel == null) {
            return NOT_AVAILABLE;
        }
        NodeModel rootNode = mapModel.getRootNode();
        if (rootNode == null) {
            return NOT_AVAILABLE;
        }
        String nodeIdentifier = rootNode.getID();
        return nodeIdentifier == null ? NOT_AVAILABLE : nodeIdentifier;
    }

    private String resolveSelectedNodeIdentifier() {
        NodeModel selectedNode = availableMaps.getCurrentSelectedNodeModel();
        if (selectedNode == null) {
            return NOT_AVAILABLE;
        }
        String nodeIdentifier = selectedNode.getID();
        return nodeIdentifier == null ? NOT_AVAILABLE : nodeIdentifier;
    }
}
