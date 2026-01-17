package org.freeplane.plugin.ai.tools.content;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TagsContentRequest {
    private final boolean includesTags;

    @JsonCreator
    public TagsContentRequest(@JsonProperty("includesTags") boolean includesTags) {
        this.includesTags = includesTags;
    }

    public boolean includesTags() {
        return includesTags;
    }
}
