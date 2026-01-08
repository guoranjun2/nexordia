package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class MoveNodesResponse {
    private final String mapIdentifier;
    private final String userSummary;
    private final List<ModifiedNodeSummary> modifiedNodes;

    @JsonCreator
    public MoveNodesResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                             @JsonProperty("userSummary") String userSummary,
                             @JsonProperty("modifiedNodes") List<ModifiedNodeSummary> modifiedNodes) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.modifiedNodes = modifiedNodes;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getUserSummary() {
        return userSummary;
    }

    public List<ModifiedNodeSummary> getModifiedNodes() {
        return modifiedNodes;
    }
}
