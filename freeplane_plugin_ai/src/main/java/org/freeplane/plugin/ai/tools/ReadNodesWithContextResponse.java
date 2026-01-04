package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ReadNodesWithContextResponse {
    private final String mapIdentifier;
    private final List<ReadNodesWithContextItem> items;

    @JsonCreator
    public ReadNodesWithContextResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                                        @JsonProperty("items") List<ReadNodesWithContextItem> items) {
        this.mapIdentifier = mapIdentifier;
        this.items = items;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<ReadNodesWithContextItem> getItems() {
        return items;
    }
}
