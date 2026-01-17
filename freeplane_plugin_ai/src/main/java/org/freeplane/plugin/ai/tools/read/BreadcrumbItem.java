package org.freeplane.plugin.ai.tools.read;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class BreadcrumbItem {
    private final String text;
    private final String nodeIdentifier;

    @JsonCreator
    public BreadcrumbItem(@JsonProperty("text") String text,
                          @JsonProperty("nodeIdentifier") String nodeIdentifier) {
        this.text = text;
        this.nodeIdentifier = nodeIdentifier;
    }

    public String getText() {
        return text;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }
}
