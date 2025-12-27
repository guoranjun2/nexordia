package org.freeplane.plugin.ai.tools;

public final class BreadcrumbsRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final boolean includeNodeIdentifiers;

    public BreadcrumbsRequest(String mapIdentifier, String nodeIdentifier, boolean includeNodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
        this.includeNodeIdentifiers = includeNodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public boolean isIncludeNodeIdentifiers() {
        return includeNodeIdentifiers;
    }
}
