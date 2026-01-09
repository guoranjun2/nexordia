package org.freeplane.plugin.ai.tools;

import java.util.List;

import org.freeplane.features.map.NodeModel;

public final class NodeContentApplier {
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
        NodeContent content = creationItem.getContent();
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

    private void guardApply(NodeModel nodeModel, NodeContent content) {
        if (content == null) {
            return;
        }
        textualContentEditor.apply(nodeModel, content.getTextualContent());
        attributesContentEditor.apply(nodeModel, content.getAttributesContent());
        tagsContentEditor.apply(nodeModel, content.getTagsContent());
        iconsContentEditor.apply(nodeModel, content.getIconsContent());
    }
}
