package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public final class AttributesContent {
    private final List<AttributeEntry> attributes;

    @JsonCreator
    public AttributesContent(@JsonProperty("attributes") List<AttributeEntry> attributes) {
        this.attributes = attributes;
    }

    public List<AttributeEntry> getAttributes() {
        return attributes;
    }
}
