package org.freeplane.plugin.ai.tools;

public final class NodeContextRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;
    private final int depth;
    private final boolean includeNotes;
    private final boolean includeDetails;
    private final boolean includeAttributes;
    private final boolean includeAncestors;
    private final boolean includeNodeIdentifiers;
    private final int maxNodes;
    private final String outputFormat;

    public NodeContextRequest(String mapIdentifier, String nodeIdentifier, int depth, boolean includeNotes,
                              boolean includeDetails, boolean includeAttributes, boolean includeAncestors,
                              boolean includeNodeIdentifiers, int maxNodes, String outputFormat) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifier = nodeIdentifier;
        this.depth = depth;
        this.includeNotes = includeNotes;
        this.includeDetails = includeDetails;
        this.includeAttributes = includeAttributes;
        this.includeAncestors = includeAncestors;
        this.includeNodeIdentifiers = includeNodeIdentifiers;
        this.maxNodes = maxNodes;
        this.outputFormat = outputFormat;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public int getDepth() {
        return depth;
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

    public boolean isIncludeAncestors() {
        return includeAncestors;
    }

    public boolean isIncludeNodeIdentifiers() {
        return includeNodeIdentifiers;
    }

    public int getMaxNodes() {
        return maxNodes;
    }

    public String getOutputFormat() {
        return outputFormat;
    }
    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
