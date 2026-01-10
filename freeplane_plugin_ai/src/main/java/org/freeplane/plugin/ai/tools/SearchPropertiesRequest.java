package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchPropertiesRequest {
    private final String mapIdentifier;

    @JsonCreator
    public SearchPropertiesRequest(@JsonProperty("mapIdentifier") String mapIdentifier) {
        this.mapIdentifier = mapIdentifier;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }
}
