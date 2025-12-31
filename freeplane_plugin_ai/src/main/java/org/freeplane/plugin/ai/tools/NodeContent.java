package org.freeplane.plugin.ai.tools;

public final class NodeContent {
    private final String briefText;
    private final TextualContent textualContent;
    private final AttributesContent attributesContent;
    private final TagsContent tagsContent;

    public NodeContent(String briefText, TextualContent textualContent, AttributesContent attributesContent,
                       TagsContent tagsContent) {
        this.briefText = briefText;
        this.textualContent = textualContent;
        this.attributesContent = attributesContent;
        this.tagsContent = tagsContent;
    }

    public String getBriefText() {
        return briefText;
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
