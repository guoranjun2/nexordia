package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CreateNodesResponse {
    private final String mapIdentifier;
    private final String userSummary;
    private final List<ModifiedNodeSummary> modifiedNodes;
    private final String summaryNodeIdentifier;

    @JsonCreator
    public CreateNodesResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                               @JsonProperty("userSummary") String userSummary,
                               @JsonProperty("modifiedNodes") List<ModifiedNodeSummary> modifiedNodes,
                               @JsonProperty("summaryNodeIdentifier") String summaryNodeIdentifier) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.modifiedNodes = modifiedNodes;
        this.summaryNodeIdentifier = summaryNodeIdentifier;
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

    public String getSummaryNodeIdentifier() {
        return summaryNodeIdentifier;
    }
}
