package org.freeplane.plugin.ai.tools;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class NodeContentRequest {
    private final TextualContentRequest textualContentRequest;
    private final AttributesContentRequest attributesContentRequest;
    private final TagsContentRequest tagsContentRequest;

    @JsonCreator
    public NodeContentRequest(@JsonProperty("textualContentRequest") TextualContentRequest textualContentRequest,
                              @JsonProperty("attributesContentRequest") AttributesContentRequest attributesContentRequest,
                              @JsonProperty("tagsContentRequest") TagsContentRequest tagsContentRequest) {
        this.textualContentRequest = textualContentRequest;
        this.attributesContentRequest = attributesContentRequest;
        this.tagsContentRequest = tagsContentRequest;
    }

    public TextualContentRequest getTextualContentRequest() {
        return textualContentRequest;
    }

    public AttributesContentRequest getAttributesContentRequest() {
        return attributesContentRequest;
    }

    public TagsContentRequest getTagsContentRequest() {
        return tagsContentRequest;
    }
}
