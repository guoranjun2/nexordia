package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class FlatListResponse {
    private final String mapIdentifier;
    private final List<FlatListItem> items;

    @JsonCreator
    public FlatListResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                            @JsonProperty("items") List<FlatListItem> items) {
        this.mapIdentifier = mapIdentifier;
        this.items = items;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<FlatListItem> getItems() {
        return items;
    }
}
