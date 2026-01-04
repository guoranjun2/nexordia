package org.freeplane.plugin.ai.tools;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public final class SearchNodesRequest {
    @Description("Map identifier string.")
    private final String mapIdentifier;
    @Description("Search query string.")
    private final String queryText;
    @JsonProperty(required = false)
    @Description("List of node identifiers that restrict search to those subtrees. Default: root node.")
    private final List<String> subtreeRootNodeIdentifiers;
    @JsonProperty(required = false)
    @Description("NodeContentRequest selecting which content fields are searched. Default: includesText true only.")
    private final NodeContentRequest nodeContentRequestForSearch;
    @JsonProperty(required = false)
    @Description("Matching mode. Default: CONTAINS.")
    private final SearchMatchingMode matchingMode;
    @JsonProperty(required = false)
    @Description("Case sensitivity for matching. Default: CASE_INSENSITIVE.")
    private final SearchCaseSensitivity caseSensitivity;
    @JsonProperty(required = false)
    @Description("Result sections to include. Default: empty list.")
    private final List<SearchResultSection> resultSections;
    @JsonProperty(required = false)
    @Description("Result offset. Default: 0.")
    private final Integer offset;
    @JsonProperty(required = false)
    @Description("Maximum number of results. Default: 200.")
    private final Integer limit;
    @JsonProperty(required = false)
    @Description("Maximum total response length in characters. Default: 65536.")
    private final Integer maximumTotalTextCharacters;

    @JsonCreator
    public SearchNodesRequest(@JsonProperty("mapIdentifier") String mapIdentifier,
                              @JsonProperty("queryText") String queryText,
                              @JsonProperty("subtreeRootNodeIdentifiers") List<String> subtreeRootNodeIdentifiers,
                              @JsonProperty("nodeContentRequestForSearch") NodeContentRequest nodeContentRequestForSearch,
                              @JsonProperty("matchingMode") SearchMatchingMode matchingMode,
                              @JsonProperty("caseSensitivity") SearchCaseSensitivity caseSensitivity,
                              @JsonProperty("resultSections") List<SearchResultSection> resultSections,
                              @JsonProperty("offset") Integer offset,
                              @JsonProperty("limit") Integer limit,
                              @JsonProperty("maximumTotalTextCharacters") Integer maximumTotalTextCharacters) {
        this.mapIdentifier = mapIdentifier;
        this.queryText = queryText;
        this.subtreeRootNodeIdentifiers = subtreeRootNodeIdentifiers;
        this.nodeContentRequestForSearch = nodeContentRequestForSearch;
        this.matchingMode = matchingMode;
        this.caseSensitivity = caseSensitivity;
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

    public SearchCaseSensitivity getCaseSensitivity() {
        return caseSensitivity;
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
