package org.freeplane.plugin.ai.tools.selection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class SelectSingleNodeRequest {
    @Description("Target map ID (from getSelectedMapAndNodeIdentifiers).")
    private final String mapIdentifier;
    @Description("Node ID in the target map.")
    private final String nodeIdentifier;

    @JsonCreator
    public SelectSingleNodeRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                   @JsonProperty("nodeIdentifier") String nodeIdentifier) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }
}
