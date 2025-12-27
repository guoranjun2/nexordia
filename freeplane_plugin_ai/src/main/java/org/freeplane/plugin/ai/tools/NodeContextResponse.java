package org.freeplane.plugin.ai.tools;

public final class NodeContextResponse {
    private final String mapIdentifier;
    private final String outputFormat;
    private final String payload;

    public NodeContextResponse(String mapIdentifier, String outputFormat, String payload) {
        this.mapIdentifier = mapIdentifier;
        this.outputFormat = outputFormat;
        this.payload = payload;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public String getPayload() {
        return payload;
    }
}
