package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class MoveNodesRequest {
    private final String mapIdentifier;
    private final String userSummary;
    private final AnchorPlacement anchorPlacement;
    private final List<String> nodeIdentifiers;
    private final boolean createSummary;

    @JsonCreator
    public MoveNodesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                            @JsonProperty("userSummary") String userSummary,
                            @JsonProperty("anchorPlacement") AnchorPlacement anchorPlacement,
                            @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers,
                            @JsonProperty("createSummary") boolean createSummary) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.anchorPlacement = anchorPlacement;
        this.nodeIdentifiers = nodeIdentifiers;
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

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
    }

    public boolean createSummary() {
        return createSummary;
    }
}
