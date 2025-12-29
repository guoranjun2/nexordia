package org.freeplane.plugin.ai.tools;

public final class FlatListRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final boolean includesDescendants;
    private final NodeContentRequest nodeContentRequest;
    private final boolean includesBreadcrumbs;
    private final int maxNodes;

    public FlatListRequest(String mapIdentifier, String nodeIdentifier, boolean includesDescendants,
                           NodeContentRequest nodeContentRequest, boolean includesBreadcrumbs,
                           int maxNodes) {
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
