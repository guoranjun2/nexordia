package org.freeplane.plugin.ai.tools;

public final class ReadNodeContentRequest {
    private final String mapIdentifier;
    private final String nodeIdentifier;

    public ReadNodeContentRequest(String mapIdentifier, String nodeIdentifier) {
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
