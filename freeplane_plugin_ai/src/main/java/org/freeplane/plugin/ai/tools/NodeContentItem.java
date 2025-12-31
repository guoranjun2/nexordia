package org.freeplane.plugin.ai.tools;

import java.util.List;
import java.util.Objects;

public final class NodeContentItem {
    private final String nodeIdentifier;
    private final NodeContent content;
    private final List<String> qualifiers;

    public NodeContentItem(String nodeIdentifier, NodeContent content, List<String> qualifiers) {
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
