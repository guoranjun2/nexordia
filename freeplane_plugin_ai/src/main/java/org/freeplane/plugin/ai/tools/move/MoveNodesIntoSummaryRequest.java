package org.freeplane.plugin.ai.tools.move;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class MoveNodesIntoSummaryRequest {
    @Description("Map identifier string. Use another tool call to refresh identifiers if needed.")
    private final String mapIdentifier;
    @Description("Short user facing summary for confirmations.")
    private final String userSummary;
    @Description("Summary anchor placement with first and last summarized node identifiers. Both identifiers must "
        + "reference existing sibling nodes.")
    private final SummaryAnchorPlacement summaryAnchorPlacement;
    @Description("Ordered list of existing node identifiers to move into summary content. All nodes become children "
        + "of a new summary node created by the tool. Must be non-empty.")
    private final List<String> nodeIdentifiers;

    @JsonCreator
    public MoveNodesIntoSummaryRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                       @JsonProperty("userSummary") String userSummary,
                                       @JsonProperty("summaryAnchorPlacement") SummaryAnchorPlacement summaryAnchorPlacement,
                                       @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.summaryAnchorPlacement = summaryAnchorPlacement;
        this.nodeIdentifiers = nodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getUserSummary() {
        return userSummary;
    }

    public SummaryAnchorPlacement getSummaryAnchorPlacement() {
        return summaryAnchorPlacement;
    }

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
    }
}
