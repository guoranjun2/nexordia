package org.freeplane.plugin.ai.tools;

public final class TagsContentRequest {
    private final boolean includesTags;

    public TagsContentRequest(boolean includesTags) {
        this.includesTags = includesTags;
    }

    public boolean includesTags() {
        return includesTags;
    }
}
