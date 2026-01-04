package org.freeplane.plugin.ai.tools;

import java.util.List;
import java.util.Objects;

import org.freeplane.features.map.NodeModel;

public class NodeContentReader {
    private final TextualContentReader textualContentReader;
    private final AttributesContentReader attributesContentReader;
    private final TagsContentReader tagsContentReader;
    private final IconsContentReader iconsContentReader;

    public NodeContentReader(TextualContentReader textualContentReader,
                             AttributesContentReader attributesContentReader,
                             TagsContentReader tagsContentReader,
                             IconsContentReader iconsContentReader) {
        this.textualContentReader = Objects.requireNonNull(textualContentReader, "textualContentReader");
        this.attributesContentReader = Objects.requireNonNull(attributesContentReader, "attributesContentReader");
        this.tagsContentReader = Objects.requireNonNull(tagsContentReader, "tagsContentReader");
        this.iconsContentReader = Objects.requireNonNull(iconsContentReader, "iconsContentReader");
    }

    public NodeContent readNodeContent(NodeModel nodeModel, NodeContentPreset preset) {
        if (nodeModel == null) {
            return null;
        }
        if (preset == NodeContentPreset.BRIEF) {
            String briefText = textualContentReader.readBriefText(nodeModel);
            return new NodeContent(briefText, null, null, null, null);
        }
        TextualContent textualContent = textualContentReader.readTextualContent(nodeModel, preset);
        AttributesContent attributesContent = attributesContentReader.readAttributesContent(nodeModel, preset);
        TagsContent tagsContent = tagsContentReader.readTagsContent(nodeModel, preset);
        IconsContent iconsContent = iconsContentReader.readIconsContent(nodeModel, preset);
        return new NodeContent(null, textualContent, attributesContent, tagsContent, iconsContent);
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
        IconsContent iconsContent = iconsContentReader.readIconsContent(
            nodeModel, request.getIconsContentRequest());
        if (textualContent == null && attributesContent == null && tagsContent == null && iconsContent == null) {
            return null;
        }
        return new NodeContent(null, textualContent, attributesContent, tagsContent, iconsContent);
    }

    public List<String> collectIconSearchTerms(NodeModel nodeModel, IconsContentRequest request) {
        return iconsContentReader.collectSearchTerms(nodeModel, request);
    }
}
