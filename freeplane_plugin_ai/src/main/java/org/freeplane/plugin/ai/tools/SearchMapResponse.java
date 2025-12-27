package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class SearchMapResponse {
    private final String mapIdentifier;
    private final List<SearchResult> results;

    public SearchMapResponse(String mapIdentifier, List<SearchResult> results) {
        this.mapIdentifier = mapIdentifier;
        this.results = results;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<SearchResult> getResults() {
        return results;
    }
}
