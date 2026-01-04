package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SelectionIdentifiersResponse {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final String rootNodeIdentifier;

    @JsonCreator
    public SelectionIdentifiersResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                        @JsonProperty("nodeIdentifier") String nodeIdentifier,
                                        @JsonProperty("rootNodeIdentifier") String rootNodeIdentifier) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
        this.rootNodeIdentifier = rootNodeIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public String getRootNodeIdentifier() {
        return rootNodeIdentifier;
    }
}
