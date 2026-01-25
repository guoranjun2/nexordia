package org.freeplane.plugin.ai.tools.selection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.langchain4j.model.output.structured.Description;

public class SelectedNodeSummary {
    @Description("Selected node ID.")
    private final String nodeIdentifier;
    @Description("Short plain-text preview.")
    private final String shortText;

    @JsonCreator
    public SelectedNodeSummary(@JsonProperty("nodeIdentifier") String nodeIdentifier,
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
