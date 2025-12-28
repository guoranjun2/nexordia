package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class SearchAttributesByNameAndValueResponse {
    private final String mapIdentifier;
    private final List<String> nodeIdentifiers;

    public SearchAttributesByNameAndValueResponse(String mapIdentifier, List<String> nodeIdentifiers) {
        this.mapIdentifier = mapIdentifier;
        this.nodeIdentifiers = nodeIdentifiers;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
    }
}
