package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class SearchNodesByConditionResponse {
    private final String mapIdentifier;
    private final List<String> nodeIdentifiers;

    public SearchNodesByConditionResponse(String mapIdentifier, List<String> nodeIdentifiers) {
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
