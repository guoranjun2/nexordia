package org.freeplane.plugin.ai.tools;

public final class AttributeNamesForMapRequest {
    private final String mapIdentifier;

    public AttributeNamesForMapRequest(String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
