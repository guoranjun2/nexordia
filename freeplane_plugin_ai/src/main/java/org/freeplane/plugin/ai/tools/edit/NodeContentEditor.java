package org.freeplane.plugin.ai.tools.edit;

import java.util.List;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.tools.content.NodeContentItem;
import org.freeplane.plugin.ai.tools.content.NodeContentItemReader;
import org.freeplane.plugin.ai.tools.content.NodeContentPreset;

public class NodeContentEditor {
    private final TextController textController;
    private final NodeContentItemReader nodeContentItemReader;
    private final TextualContentEditor textualContentEditor;
    private final AttributesContentEditor attributesContentEditor;
    private final TagsContentEditor tagsContentEditor;
    private final IconsContentEditor iconsContentEditor;

    public NodeContentEditor(TextController textController, NodeContentItemReader nodeContentItemReader,
                             TextualContentEditor textualContentEditor,
                             AttributesContentEditor attributesContentEditor,
                             TagsContentEditor tagsContentEditor,
                             IconsContentEditor iconsContentEditor) {
        this.textController = textController;
        this.nodeContentItemReader = nodeContentItemReader;
        this.textualContentEditor = textualContentEditor;
        this.attributesContentEditor = attributesContentEditor;
        this.tagsContentEditor = tagsContentEditor;
        this.iconsContentEditor = iconsContentEditor;
    }

    public NodeContentItem edit(NodeModel nodeModel, List<NodeContentEditItem> items) {
        if (nodeModel == null) {
            throw new IllegalArgumentException("Missing node model.");
        }
        if (items == null || items.isEmpty()) {
            return nodeContentItemReader.readNodeContentItem(nodeModel, NodeContentPreset.FULL);
        }
        for (NodeContentEditItem edit : items) {
            applyEdit(nodeModel, edit);
        }
        return nodeContentItemReader.readNodeContentItem(nodeModel, NodeContentPreset.FULL);
    }

    private void applyEdit(NodeModel nodeModel, NodeContentEditItem edit) {
        if (edit == null) {
            return;
        }
        EditedElement editedElement = edit.getEditedElement();
        if (editedElement == null) {
            throw new IllegalArgumentException("Missing edited element.");
        }
        switch (editedElement) {
            case TEXT:
                applyTextualContent(nodeModel, edit);
                break;
            case DETAILS:
                applyTextualContent(nodeModel, edit);
                break;
            case NOTE:
                applyTextualContent(nodeModel, edit);
                break;
            case ATTRIBUTES:
                applyAttributes(nodeModel, edit);
                break;
            case TAGS:
                applyTags(nodeModel, edit);
                break;
            case ICONS:
                applyIcons(nodeModel, edit);
                break;
            default:
                throw new IllegalArgumentException("Unknown edited element: " + editedElement);
        }
    }

    private void applyTextualContent(NodeModel nodeModel, NodeContentEditItem edit) {
        ensureReplace(edit);
        textualContentEditor.editExistingTextualContent(
            nodeModel,
            edit.getEditedElement(),
            edit.getOriginalContentType(),
            edit.getValue(),
            textController);
    }

    private void applyAttributes(NodeModel nodeModel, NodeContentEditItem edit) {
        attributesContentEditor.editExistingAttributesContent(
            nodeModel,
            edit.getOperation(),
            edit.getTargetKey(),
            edit.getIndex(),
            edit.getValue());
    }

    private void applyTags(NodeModel nodeModel, NodeContentEditItem edit) {
        tagsContentEditor.editExistingTagsContent(
            nodeModel,
            edit.getOperation(),
            edit.getTargetKey(),
            edit.getIndex(),
            edit.getValue());
    }

    private void applyIcons(NodeModel nodeModel, NodeContentEditItem edit) {
        iconsContentEditor.editExistingIconsContent(
            nodeModel,
            edit.getOperation(),
            edit.getTargetKey(),
            edit.getIndex(),
            edit.getValue());
    }

    private void ensureReplace(NodeContentEditItem edit) {
        if (edit.getOperation() != EditOperation.REPLACE) {
            throw new IllegalArgumentException("Only REPLACE operations are supported for this element.");
        }
    }
}
