package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class MoveNodesRequest {
    private final String mapIdentifier;
    private final String userSummary;
    private final AnchorPlacement anchorPlacement;
    private final SummaryAnchorPlacement summaryAnchorPlacement;
    private final List<String> nodeIdentifiers;

    @JsonCreator
    public MoveNodesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                            @JsonProperty("userSummary") String userSummary,
                            @JsonProperty("anchorPlacement") AnchorPlacement anchorPlacement,
                            @JsonProperty("summaryAnchorPlacement") SummaryAnchorPlacement summaryAnchorPlacement,
                            @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.anchorPlacement = anchorPlacement;
        this.summaryAnchorPlacement = summaryAnchorPlacement;
        this.nodeIdentifiers = nodeIdentifiers;
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

    public SummaryAnchorPlacement getSummaryAnchorPlacement() {
        return summaryAnchorPlacement;
    }

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
    }
}
