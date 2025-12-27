package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class CreateNodesResponse {
    private final String mapIdentifier;
    private final List<String> createdNodeIdentifiers;

    public CreateNodesResponse(String mapIdentifier, List<String> createdNodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.createdNodeIdentifiers = createdNodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getCreatedNodeIdentifiers() {
        return createdNodeIdentifiers;
    }
}
