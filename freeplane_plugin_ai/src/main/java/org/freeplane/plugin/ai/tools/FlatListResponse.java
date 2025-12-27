package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class FlatListResponse {
    private final String mapIdentifier;
    private final List<FlatListItem> items;

    public FlatListResponse(String mapIdentifier, List<FlatListItem> items) {
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
