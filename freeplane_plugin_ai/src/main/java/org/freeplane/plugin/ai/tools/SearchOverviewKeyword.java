package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class SearchOverviewKeyword {
    private final String term;
    private final List<String> nodeIdentifiers;

    public SearchOverviewKeyword(String term, List<String> nodeIdentifiers) {
        this.term = term;
        this.nodeIdentifiers = nodeIdentifiers;
    }

    public String getTerm() {
        return term;
    }

    public List<String> getNodeIdentifiers() {
        return nodeIdentifiers;
    }
}
