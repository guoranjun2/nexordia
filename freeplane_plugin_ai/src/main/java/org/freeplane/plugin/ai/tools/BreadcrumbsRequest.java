package org.freeplane.plugin.ai.tools;

public final class BreadcrumbsRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final boolean includesNodeIdentifiers;

    public BreadcrumbsRequest(String mapIdentifier, String nodeIdentifier, boolean includesNodeIdentifiers) {
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
