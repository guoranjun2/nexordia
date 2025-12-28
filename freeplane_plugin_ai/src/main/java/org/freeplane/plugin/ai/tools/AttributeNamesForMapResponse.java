package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class AttributeNamesForMapResponse {
    private final String mapIdentifier;
    private final List<String> attributeNames;

    public AttributeNamesForMapResponse(String mapIdentifier, List<String> attributeNames) {
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
