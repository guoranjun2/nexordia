package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SelectionIdentifiersResponse {
    private final String mapIdentifier;
    private final String nodeIdentifier;

    @JsonCreator
    public SelectionIdentifiersResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
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
