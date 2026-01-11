package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NodeContentEditRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final String userSummary;
    private final List<NodeContentEditItem> edits;

    @JsonCreator
    public NodeContentEditRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                  @JsonProperty("nodeIdentifier") String nodeIdentifier,
                                  @JsonProperty("userSummary") String userSummary,
                                  @JsonProperty("edits") List<NodeContentEditItem> edits) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
        this.userSummary = userSummary;
        this.edits = edits;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public String getUserSummary() {
        return userSummary;
    }

    public List<NodeContentEditItem> getEdits() {
        return edits;
    }
}
