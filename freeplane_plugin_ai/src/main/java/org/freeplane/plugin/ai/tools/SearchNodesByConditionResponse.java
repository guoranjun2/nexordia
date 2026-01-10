package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchNodesByConditionResponse {
    private final String mapIdentifier;
    private final List<String> nodeIdentifiers;

    @JsonCreator
    public SearchNodesByConditionResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                          @JsonProperty("nodeIdentifiers") List<String> nodeIdentifiers) {
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
