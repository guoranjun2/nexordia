package org.freeplane.plugin.ai.tools.read;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BreadcrumbsRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final boolean includesNodeIdentifiers;

    @JsonCreator
    public BreadcrumbsRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                              @JsonProperty("nodeIdentifier") String nodeIdentifier,
                              @JsonProperty("includesNodeIdentifiers") boolean includesNodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
        this.includesNodeIdentifiers = includesNodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public boolean includesNodeIdentifiers() {
        return includesNodeIdentifiers;
    }
}
