package org.freeplane.plugin.ai.tools;

import org.freeplane.features.map.NodeModel;

public class NodeContentApplier {
    private final TextualContentEditor textualContentEditor;
    private final AttributesContentEditor attributesContentEditor;
    private final TagsContentEditor tagsContentEditor;
    private final IconsContentEditor iconsContentEditor;

    public NodeContentApplier(TextualContentEditor textualContentEditor,
                              AttributesContentEditor attributesContentEditor,
                              TagsContentEditor tagsContentEditor,
                              IconsContentEditor iconsContentEditor) {
        this.textualContentEditor = textualContentEditor;
        this.attributesContentEditor = attributesContentEditor;
        this.tagsContentEditor = tagsContentEditor;
        this.iconsContentEditor = iconsContentEditor;
    }

    public void apply(NodeModel nodeModel, NodeContentWriteRequest content) {
        if (nodeModel == null || content == null) {
            return;
        }
        guardApply(nodeModel, content);
    }

    private void guardApply(NodeModel nodeModel, NodeContentWriteRequest content) {
        if (content == null) {
            return;
        }
        textualContentEditor.setInitialContent(nodeModel, content);
        attributesContentEditor.setInitialContent(nodeModel, toAttributesContent(content));
        tagsContentEditor.setInitialContent(nodeModel, toTagsContent(content));
        iconsContentEditor.setInitialContent(nodeModel, toIconsContent(content));
    }

    private AttributesContent toAttributesContent(NodeContentWriteRequest content) {
        if (content.getAttributes() == null) {
            return null;
        }
        return new AttributesContent(content.getAttributes());
    }

    private TagsContent toTagsContent(NodeContentWriteRequest content) {
        if (content.getTags() == null) {
            return null;
        }
        return new TagsContent(content.getTags());
    }

    private IconsContent toIconsContent(NodeContentWriteRequest content) {
        if (content.getIcons() == null) {
            return null;
        }
        return new IconsContent(content.getIcons());
    }
}
