package org.freeplane.plugin.ai.tools;

import java.util.List;

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

    public void apply(NodeModel nodeModel, NodeCreationItem creationItem) {
        if (nodeModel == null || creationItem == null) {
            return;
        }
        NodeContentWriteRequest content = creationItem.getContent();
        guardApply(nodeModel, content);
        List<NodeCreationItem> children = creationItem.getChildren();
        if (children == null || children.isEmpty()) {
            return;
        }
        for (int index = 0; index < children.size(); index++) {
            NodeCreationItem childItem = children.get(index);
            if (index < nodeModel.getChildCount()) {
                NodeModel childNode = nodeModel.getChildAt(index);
                apply(childNode, childItem);
            }
        }
    }

    private void guardApply(NodeModel nodeModel, NodeContentWriteRequest content) {
        if (content == null) {
            return;
        }
        textualContentEditor.setInitialContent(nodeModel, toTextualContent(content));
        attributesContentEditor.setInitialContent(nodeModel, toAttributesContent(content));
        tagsContentEditor.setInitialContent(nodeModel, toTagsContent(content));
        iconsContentEditor.setInitialContent(nodeModel, toIconsContent(content));
    }

    private TextualContent toTextualContent(NodeContentWriteRequest content) {
        if (content.getText() == null && content.getDetails() == null && content.getNote() == null) {
            return null;
        }
        return new TextualContent(content.getText(), content.getDetails(), content.getNote());
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
