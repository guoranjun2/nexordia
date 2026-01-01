package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public final class NodeContentItem {
    private final String nodeIdentifier;
    private final NodeContent content;
    private final List<String> qualifiers;

    @JsonCreator
    public NodeContentItem(@JsonProperty("nodeIdentifier") String nodeIdentifier,
                           @JsonProperty("content") NodeContent content,
                           @JsonProperty("qualifiers") List<String> qualifiers) {
        this.nodeIdentifier = nodeIdentifier;
        this.content = content;
        this.qualifiers = Objects.requireNonNull(qualifiers, "qualifiers");
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public NodeContent getContent() {
        return content;
    }

    public List<String> getQualifiers() {
        return qualifiers;
    }
}
