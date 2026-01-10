package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AttributeNamesForMapResponse {
    private final String mapIdentifier;
    private final List<String> attributeNames;

    @JsonCreator
    public AttributeNamesForMapResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                        @JsonProperty("attributeNames") List<String> attributeNames) {
        this.mapIdentifier = mapIdentifier;
        this.attributeNames = attributeNames;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getAttributeNames() {
        return attributeNames;
    }
}
