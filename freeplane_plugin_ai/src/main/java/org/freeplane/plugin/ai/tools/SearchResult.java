package org.freeplane.plugin.ai.tools;

import java.util.Map;

public final class SearchResult {
    private final String nodeIdentifier;
    private final NodeContent content;

    public SearchResult(String nodeIdentifier, NodeContent content) {
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
