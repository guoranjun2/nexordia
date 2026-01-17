package org.freeplane.plugin.ai.tools.delete;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class DeleteNodesRequest {
    @Description("Map identifier string. Use another tool call to refresh identifiers if needed.")
    private final String mapIdentifier;
    @Description("List of node identifiers to delete.")
    private final List<String> nodeIdentifiers;
    @Description("Short user facing summary for confirmations.")
    private final String userSummary;

    @JsonCreator
    public DeleteNodesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                              @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers,
                              @JsonProperty("userSummary") String userSummary) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifiers = nodeIdentifiers;
        this.userSummary = userSummary;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
    }

    public String getUserSummary() {
        return userSummary;
    }
}
