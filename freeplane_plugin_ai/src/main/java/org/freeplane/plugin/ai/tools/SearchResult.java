package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class SearchResult {
    private final String nodeIdentifier;
    private final NodeContent content;

    @JsonCreator
    public SearchResult(@JsonProperty("nodeIdentifier") String nodeIdentifier,
                        @JsonProperty("content") NodeContent content) {
        this.nodeIdentifier = nodeIdentifier;
        this.content = content;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public NodeContent getContent() {
        return content;
    }
}
