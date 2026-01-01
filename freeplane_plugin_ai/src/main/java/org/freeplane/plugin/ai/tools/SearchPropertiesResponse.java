package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SearchPropertiesResponse {
    private final String mapIdentifier;
    private final List<SearchPropertyDefinition> properties;

    @JsonCreator
    public SearchPropertiesResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                    @JsonProperty("properties") List<SearchPropertyDefinition> properties) {
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
