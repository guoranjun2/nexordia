package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class SummaryAnchorPlacement {
    private final String firstNodeIdentifier;
    private final String lastNodeIdentifier;

    @JsonCreator
    public SummaryAnchorPlacement(@JsonProperty("firstNodeIdentifier") String firstNodeIdentifier,
                                  @JsonProperty("lastNodeIdentifier") String lastNodeIdentifier) {
        this.firstNodeIdentifier = firstNodeIdentifier;
        this.lastNodeIdentifier = lastNodeIdentifier;
    }

    public String getFirstNodeIdentifier() {
        return firstNodeIdentifier;
    }

    public String getLastNodeIdentifier() {
        return lastNodeIdentifier;
    }
}
