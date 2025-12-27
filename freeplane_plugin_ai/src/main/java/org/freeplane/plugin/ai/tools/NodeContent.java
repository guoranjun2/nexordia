package org.freeplane.plugin.ai.tools;

import java.util.Map;

public final class NodeContent {
    private final TextualContent textualContent;
    private final AttributesContent attributesContent;
    private final TagsContent tagsContent;

    public NodeContent(TextualContent textualContent, AttributesContent attributesContent, TagsContent tagsContent) {
        this.textualContent = textualContent;
        this.attributesContent = attributesContent;
        this.tagsContent = tagsContent;
    }

    public TextualContent getTextualContent() {
        return textualContent;
    }

    public AttributesContent getAttributesContent() {
        return attributesContent;
    }

    public TagsContent getTagsContent() {
        return tagsContent;
    }
}
