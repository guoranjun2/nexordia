package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import org.freeplane.features.map.NodeModel;

public class NodeContentReader {
    private final TextualContentReader textualContentReader;
    private final AttributesContentReader attributesContentReader;
    private final TagsContentReader tagsContentReader;

    public NodeContentReader(TextualContentReader textualContentReader,
                             AttributesContentReader attributesContentReader,
                             TagsContentReader tagsContentReader) {
        this.textualContentReader = Objects.requireNonNull(textualContentReader, "textualContentReader");
        this.attributesContentReader = Objects.requireNonNull(attributesContentReader, "attributesContentReader");
        this.tagsContentReader = Objects.requireNonNull(tagsContentReader, "tagsContentReader");
    }

    public NodeContent readNodeContent(NodeModel nodeModel, NodeContentPreset preset) {
        if (nodeModel == null) {
            return null;
        }
        if (preset == NodeContentPreset.BRIEF) {
            String briefText = textualContentReader.readBriefText(nodeModel);
            return new NodeContent(briefText, null, null, null);
        }
        TextualContent textualContent = textualContentReader.readTextualContent(nodeModel, preset);
        AttributesContent attributesContent = attributesContentReader.readAttributesContent(nodeModel, preset);
        TagsContent tagsContent = tagsContentReader.readTagsContent(nodeModel, preset);
        return new NodeContent(null, textualContent, attributesContent, tagsContent);
    }

    public NodeContent readNodeContent(NodeModel nodeModel, NodeContentRequest request, NodeContentPreset fallbackPreset) {
        if (nodeModel == null) {
            return null;
        }
        if (request == null) {
            return readNodeContent(nodeModel, fallbackPreset);
        }
        TextualContent textualContent = textualContentReader.readTextualContent(
            nodeModel, request.getTextualContentRequest());
        AttributesContent attributesContent = attributesContentReader.readAttributesContent(
            nodeModel, request.getAttributesContentRequest());
        TagsContent tagsContent = tagsContentReader.readTagsContent(
            nodeModel, request.getTagsContentRequest());
        if (textualContent == null && attributesContent == null && tagsContent == null) {
            return null;
        }
        return new NodeContent(null, textualContent, attributesContent, tagsContent);
    }
}
