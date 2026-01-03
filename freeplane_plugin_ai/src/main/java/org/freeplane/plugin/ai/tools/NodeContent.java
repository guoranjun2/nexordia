package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class NodeContent {
    private final String briefText;
    private final TextualContent textualContent;
    private final AttributesContent attributesContent;
    private final TagsContent tagsContent;

    @JsonCreator
    public NodeContent(@JsonProperty("briefText") String briefText,
                       @JsonProperty("textualContent") TextualContent textualContent,
                       @JsonProperty("attributesContent") AttributesContent attributesContent,
                       @JsonProperty("tagsContent") TagsContent tagsContent) {
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
