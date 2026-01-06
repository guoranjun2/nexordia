package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class ModifiedNodeSummary {
    private final String nodeIdentifier;
    private final String shortText;

    @JsonCreator
    public ModifiedNodeSummary(@JsonProperty("nodeIdentifier") String nodeIdentifier,
                               @JsonProperty("shortText") String shortText) {
        this.nodeIdentifier = nodeIdentifier;
        this.shortText = shortText;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public String getShortText() {
        return shortText;
    }
}
