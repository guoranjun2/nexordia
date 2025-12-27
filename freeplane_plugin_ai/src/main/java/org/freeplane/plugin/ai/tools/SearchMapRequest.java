package org.freeplane.plugin.ai.tools;

public final class SearchMapRequest {
    private final String mapIdentifier;
    private final String queryText;
    private final String scopeNodeIdentifier;
    private final boolean includeNotes;
    private final boolean includeDetails;
    private final boolean includeAttributes;
    private final String matchMode;
    private final boolean caseSensitive;
    private final int maxResults;

    public SearchMapRequest(String mapIdentifier, String queryText, String scopeNodeIdentifier, boolean includeNotes,
                            boolean includeDetails, boolean includeAttributes, String matchMode,
                            boolean caseSensitive, int maxResults) {
        this.mapIdentifier = mapIdentifier;
        this.queryText = queryText;
        this.scopeNodeIdentifier = scopeNodeIdentifier;
        this.includeNotes = includeNotes;
        this.includeDetails = includeDetails;
        this.includeAttributes = includeAttributes;
        this.matchMode = matchMode;
        this.caseSensitive = caseSensitive;
        this.maxResults = maxResults;
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getQueryText() {
        return queryText;
    }

    public String getScopeNodeIdentifier() {
        return scopeNodeIdentifier;
    }

    public boolean isIncludeNotes() {
        return includeNotes;
    }

    public boolean isIncludeDetails() {
        return includeDetails;
    }

    public boolean isIncludeAttributes() {
        return includeAttributes;
    }

    public String getMatchMode() {
        return matchMode;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public int getMaxResults() {
        return maxResults;
    }
}
