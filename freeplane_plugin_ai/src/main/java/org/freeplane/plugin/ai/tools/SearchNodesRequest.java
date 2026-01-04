package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SearchNodesRequest {
    private final String mapIdentifier;
    private final String queryText;
    private final List<String> subtreeRootNodeIdentifiers;
    private final NodeContentRequest nodeContentRequestForSearch;
    private final SearchMatchingMode matchingMode;
    private final List<SearchResultSection> resultSections;
    private final Integer offset;
    private final Integer limit;
    private final Integer maximumTotalTextCharacters;

    @JsonCreator
    public SearchNodesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                              @JsonProperty("queryText") String queryText,
                              @JsonProperty("subtreeRootNodeIdentifiers") List<String> subtreeRootNodeIdentifiers,
                              @JsonProperty("nodeContentRequestForSearch") NodeContentRequest nodeContentRequestForSearch,
                              @JsonProperty("matchingMode") SearchMatchingMode matchingMode,
                              @JsonProperty("resultSections") List<SearchResultSection> resultSections,
                              @JsonProperty("offset") Integer offset,
                              @JsonProperty("limit") Integer limit,
                              @JsonProperty("maximumTotalTextCharacters") Integer maximumTotalTextCharacters) {
        this.mapIdentifier = mapIdentifier;
        this.queryText = queryText;
        this.subtreeRootNodeIdentifiers = subtreeRootNodeIdentifiers;
        this.nodeContentRequestForSearch = nodeContentRequestForSearch;
        this.matchingMode = matchingMode;
        this.resultSections = resultSections;
        this.offset = offset;
        this.limit = limit;
        this.maximumTotalTextCharacters = maximumTotalTextCharacters;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getQueryText() {
        return queryText;
    }

    public List<String> getSubtreeRootNodeIdentifiers() {
        return subtreeRootNodeIdentifiers;
    }

    public NodeContentRequest getNodeContentRequestForSearch() {
        return nodeContentRequestForSearch;
    }

    public SearchMatchingMode getMatchingMode() {
        return matchingMode;
    }

    public List<SearchResultSection> getResultSections() {
        return resultSections;
    }

    public Integer getOffset() {
        return offset;
    }

    public Integer getLimit() {
        return limit;
    }

    public Integer getMaximumTotalTextCharacters() {
        return maximumTotalTextCharacters;
    }
}
