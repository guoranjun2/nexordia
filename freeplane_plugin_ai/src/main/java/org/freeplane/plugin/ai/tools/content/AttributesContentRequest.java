package org.freeplane.plugin.ai.tools.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AttributesContentRequest {
    private final boolean includesAttributes;

    @JsonCreator
    public AttributesContentRequest(@JsonProperty("includesAttributes") boolean includesAttributes) {
        this.includesAttributes = includesAttributes;
    }

    public boolean includesAttributes() {
        return includesAttributes;
    }
}
