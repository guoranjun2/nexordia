package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchAttributesByNameAndValueRequest {
    private final String mapIdentifier;
    private final String attributeName;
    private final String attributeValue;

    @JsonCreator
    public SearchAttributesByNameAndValueRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                                                 @JsonProperty("attributeName") String attributeName,
                                                 @JsonProperty("attributeValue") String attributeValue) {
        this.mapIdentifier = mapIdentifier;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }
}
