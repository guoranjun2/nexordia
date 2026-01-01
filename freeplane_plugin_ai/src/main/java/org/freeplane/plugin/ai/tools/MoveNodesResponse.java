package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class MoveNodesResponse {
    private final String mapIdentifier;
    private final List<String> movedNodeIdentifiers;

    @JsonCreator
    public MoveNodesResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                             @JsonProperty("movedNodeIdentifiers") List<String> movedNodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.movedNodeIdentifiers = movedNodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getMovedNodeIdentifiers() {
        return movedNodeIdentifiers;
    }
}
