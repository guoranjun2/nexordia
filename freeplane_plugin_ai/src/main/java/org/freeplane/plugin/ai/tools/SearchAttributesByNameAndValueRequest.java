package org.freeplane.plugin.ai.tools;

public final class SearchAttributesByNameAndValueRequest {
    private final String mapIdentifier;
    private final String attributeName;
    private final String attributeValue;

    public SearchAttributesByNameAndValueRequest(String mapIdentifier, String attributeName, String attributeValue) {
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
