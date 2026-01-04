package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class SearchNodesResponse {
    private final String mapIdentifier;
    private final List<SearchResultItem> results;
    private final Omissions omissions;

    @JsonCreator
    public SearchNodesResponse(@JsonProperty("mapIdentifier") String mapIdentifier,
                               @JsonProperty("results") List<SearchResultItem> results,
                               @JsonProperty("omissions") Omissions omissions) {
        this.mapIdentifier = mapIdentifier;
        this.results = results;
        this.omissions = omissions;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public List<SearchResultItem> getResults() {
        return results;
    }

    public Omissions getOmissions() {
        return omissions;
    }
}
