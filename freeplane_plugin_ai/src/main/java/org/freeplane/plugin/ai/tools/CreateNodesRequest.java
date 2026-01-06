package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CreateNodesRequest {
    private final String mapIdentifier;
    private final String userSummary;
    private final AnchorPlacement anchorPlacement;
    private final List<NodeCreationItem> nodes;
    private final boolean createSummary;

    @JsonCreator
    public CreateNodesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                              @JsonProperty("userSummary") String userSummary,
                              @JsonProperty("anchorPlacement") AnchorPlacement anchorPlacement,
                              @JsonProperty("nodes") List<NodeCreationItem> nodes,
                              @JsonProperty("createSummary") boolean createSummary) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.anchorPlacement = anchorPlacement;
        this.nodes = nodes;
        this.createSummary = createSummary;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getUserSummary() {
        return userSummary;
    }

    public AnchorPlacement getAnchorPlacement() {
        return anchorPlacement;
    }

    public List<NodeCreationItem> getNodes() {
        return nodes;
    }

    public boolean createSummary() {
        return createSummary;
    }
}
