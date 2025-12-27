package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class ApplyAttributesResponse {
    private final String mapIdentifier;
    private final List<String> updatedNodeIdentifiers;

    public ApplyAttributesResponse(String mapIdentifier, List<String> updatedNodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.updatedNodeIdentifiers = updatedNodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getUpdatedNodeIdentifiers() {
        return updatedNodeIdentifiers;
    }
}
