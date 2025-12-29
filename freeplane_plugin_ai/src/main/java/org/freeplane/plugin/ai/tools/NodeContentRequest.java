package org.freeplane.plugin.ai.tools;

public final class NodeContentRequest {
    private final TextualContentRequest textualContentRequest;
    private final AttributesContentRequest attributesContentRequest;
    private final TagsContentRequest tagsContentRequest;

    public NodeContentRequest(TextualContentRequest textualContentRequest,
                              AttributesContentRequest attributesContentRequest,
                              TagsContentRequest tagsContentRequest) {
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
