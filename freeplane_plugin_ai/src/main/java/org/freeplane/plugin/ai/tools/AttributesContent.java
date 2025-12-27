package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class AttributesContent {
    private final List<AttributeEntry> attributes;

    public AttributesContent(List<AttributeEntry> attributes) {
        this.attributes = attributes;
    }

    public List<AttributeEntry> getAttributes() {
        return attributes;
    }
}
