package org.freeplane.plugin.ai.tools;

import java.util.Objects;
import java.util.UUID;

import org.freeplane.features.map.NodeModel;
import org.freeplane.plugin.ai.maps.AvailableMaps;

public class SelectedMapAndNodeIdentifiersTool {
    private final AvailableMaps availableMaps;

    public SelectedMapAndNodeIdentifiersTool(AvailableMaps availableMaps) {
        this.availableMaps = Objects.requireNonNull(availableMaps, "availableMaps");
    }

    public SelectionIdentifiersResponse getSelectedMapAndNodeIdentifiers() {
        UUID mapIdentifier = availableMaps.getCurrentMapIdentifier();
        NodeModel selectedNode = availableMaps.getCurrentSelectedNodeModel();
        String nodeIdentifier = selectedNode == null ? null : selectedNode.getID();
        String mapIdentifierValue = mapIdentifier == null ? null : mapIdentifier.toString();
        return new SelectionIdentifiersResponse(mapIdentifierValue, nodeIdentifier);
    }
}
