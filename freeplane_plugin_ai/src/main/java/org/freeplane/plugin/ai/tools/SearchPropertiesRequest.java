package org.freeplane.plugin.ai.tools;

public final class SearchPropertiesRequest {
    private final String mapIdentifier;

    public SearchPropertiesRequest(String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
