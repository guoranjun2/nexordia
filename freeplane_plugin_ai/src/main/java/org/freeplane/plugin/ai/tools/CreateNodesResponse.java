package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CreateNodesResponse {
    private final String mapIdentifier;
    private final List<String> createdNodeIdentifiers;

    @JsonCreator
    public CreateNodesResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                               @JsonProperty("createdNodeIdentifiers") List<String> createdNodeIdentifiers) {
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
