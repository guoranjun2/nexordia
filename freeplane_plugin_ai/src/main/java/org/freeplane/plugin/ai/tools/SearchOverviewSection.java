package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class SearchOverviewSection {
    private final String nodeIdentifier;
    private final String nodeText;
    private final List<String> keywords;

    public SearchOverviewSection(String nodeIdentifier, String nodeText, List<String> keywords) {
        this.nodeIdentifier = nodeIdentifier;
        this.nodeText = nodeText;
        this.keywords = keywords;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public String getNodeText() {
        return nodeText;
    }

    public List<String> getKeywords() {
        return keywords;
    }
}
