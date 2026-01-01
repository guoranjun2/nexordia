package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class FlatListRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final boolean includesDescendants;
    private final NodeContentRequest nodeContentRequest;
    private final boolean includesBreadcrumbs;
    private final int maxNodes;

    @JsonCreator
    public FlatListRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                           @JsonProperty("nodeIdentifier") String nodeIdentifier,
                           @JsonProperty("includesDescendants") boolean includesDescendants,
                           @JsonProperty("nodeContentRequest") NodeContentRequest nodeContentRequest,
                           @JsonProperty("includesBreadcrumbs") boolean includesBreadcrumbs,
                           @JsonProperty("maxNodes") int maxNodes) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
        this.includesDescendants = includesDescendants;
        this.nodeContentRequest = nodeContentRequest;
        this.includesBreadcrumbs = includesBreadcrumbs;
        this.maxNodes = maxNodes;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public boolean includesDescendants() {
        return includesDescendants;
    }

    public NodeContentRequest getNodeContentRequest() {
        return nodeContentRequest;
    }

    public boolean includesBreadcrumbs() {
        return includesBreadcrumbs;
    }

    public int getMaxNodes() {
        return maxNodes;
    }
}
