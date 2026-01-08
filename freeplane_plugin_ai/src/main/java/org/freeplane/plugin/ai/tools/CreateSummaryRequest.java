package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public final class CreateSummaryRequest {
    @Description("Map identifier string. Use another tool call to refresh identifiers if needed.")
    private final String mapIdentifier;
    @Description("Short user facing summary for confirmations.")
    private final String userSummary;
    @Description("Summary anchor placement with first and last summarized node identifiers. Both identifiers must "
        + "reference existing sibling nodes.")
    private final SummaryAnchorPlacement summaryAnchorPlacement;
    @Description("Summary content nodes that become children of a new summary node. The summary node is created by the "
        + "tool and has no content. Must be non-empty.")
    private final List<NodeCreationItem> nodes;

    @JsonCreator
    public CreateSummaryRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                @JsonProperty("userSummary") String userSummary,
                                @JsonProperty("summaryAnchorPlacement") SummaryAnchorPlacement summaryAnchorPlacement,
                                @JsonProperty("nodes") List<NodeCreationItem> nodes) {
        this.mapIdentifier = mapIdentifier;
        this.userSummary = userSummary;
        this.summaryAnchorPlacement = summaryAnchorPlacement;
        this.nodes = nodes;
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

    public List<NodeCreationItem> getNodes() {
        return nodes;
    }
}
