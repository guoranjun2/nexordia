package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class SearchPropertiesResponse {
    private final String mapIdentifier;
    private final List<SearchPropertyDefinition> properties;

    public SearchPropertiesResponse(String mapIdentifier, List<SearchPropertyDefinition> properties) {
        this.mapIdentifier = mapIdentifier;
        this.properties = properties;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<SearchPropertyDefinition> getProperties() {
        return properties;
    }
}
