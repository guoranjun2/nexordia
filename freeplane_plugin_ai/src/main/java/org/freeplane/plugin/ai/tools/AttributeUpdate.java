package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class AttributeUpdate {
    private final String nodeIdentifier;
    private final List<AttributeEntry> attributes;

    public AttributeUpdate(String nodeIdentifier, List<AttributeEntry> attributes) {
        this.nodeIdentifier = nodeIdentifier;
        this.attributes = attributes;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier;
    }

    public List<AttributeEntry> getAttributes() {
        return attributes;
    }
}
