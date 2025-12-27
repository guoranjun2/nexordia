package org.freeplane.plugin.ai.tools;

import java.util.List;

public final class TagsContent {
    private final List<String> tags;

    public TagsContent(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTags() {
        return tags;
    }
}
