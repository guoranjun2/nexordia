package org.freeplane.plugin.ai.tools;

public final class FlatListRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final boolean recursive;
    private final boolean includeNotes;
    private final boolean includeDetails;
    private final boolean includeAttributes;
    private final boolean includeBreadcrumbs;
    private final int maxNodes;

    public FlatListRequest(String mapIdentifier, String nodeIdentifier, boolean recursive, boolean includeNotes,
                           boolean includeDetails, boolean includeAttributes, boolean includeBreadcrumbs,
                           int maxNodes) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
        this.recursive = recursive;
        this.includeNotes = includeNotes;
        this.includeDetails = includeDetails;
        this.includeAttributes = includeAttributes;
        this.includeBreadcrumbs = includeBreadcrumbs;
        this.maxNodes = maxNodes;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public boolean isIncludeNotes() {
        return includeNotes;
    }

    public boolean isIncludeDetails() {
        return includeDetails;
    }

    public boolean isIncludeAttributes() {
        return includeAttributes;
    }

    public boolean isIncludeBreadcrumbs() {
        return includeBreadcrumbs;
    }

    public int getMaxNodes() {
        return maxNodes;
    }
}
