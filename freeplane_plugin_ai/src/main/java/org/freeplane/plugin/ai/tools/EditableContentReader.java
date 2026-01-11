package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.attribute.Attribute;
import org.freeplane.features.attribute.NodeAttributeTableModel;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.TagReference;
import org.freeplane.features.icon.Tags;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.TextController;

public class EditableContentReader {
    private final TextController textController;
    private final IconDescriptionResolver iconDescriptionResolver;
    private final ContentTypeConverter contentTypeConverter;

    public EditableContentReader(TextController textController, IconDescriptionResolver iconDescriptionResolver,
                                 ContentTypeConverter contentTypeConverter) {
        this.textController = Objects.requireNonNull(textController, "textController");
        this.iconDescriptionResolver = Objects.requireNonNull(iconDescriptionResolver, "iconDescriptionResolver");
        this.contentTypeConverter = Objects.requireNonNull(contentTypeConverter, "contentTypeConverter");
    }

    public EditableContent readEditableContent(NodeModel nodeModel, EditableContentRequest request) {
        if (nodeModel == null || request == null) {
            return null;
        }
        EditableText editableText = request.includesField(EditableContentField.TEXT)
            ? buildEditableTextForNodeText(nodeModel, request)
            : null;
        EditableText editableDetails = request.includesField(EditableContentField.DETAILS)
            ? buildEditableText(DetailModel.getDetailText(nodeModel),
                DetailModel.getDetailContentType(nodeModel), nodeModel, DetailModel.getDetail(nodeModel), request)
            : null;
        EditableText editableNote = request.includesField(EditableContentField.NOTE)
            ? buildEditableText(NoteModel.getNoteText(nodeModel),
                NoteModel.getNoteContentType(nodeModel), nodeModel, NoteModel.getNote(nodeModel), request)
            : null;
        List<EditableAttribute> editableAttributes = request.includesField(EditableContentField.ATTRIBUTES)
            ? buildEditableAttributes(nodeModel, request)
            : null;
        List<EditableTag> editableTags = request.includesField(EditableContentField.TAGS)
            ? buildEditableTags(nodeModel, request)
            : null;
        List<EditableIcon> editableIcons = request.includesField(EditableContentField.ICONS)
            ? buildEditableIcons(nodeModel, request)
            : null;
        if (editableText == null && editableDetails == null && editableNote == null
            && editableAttributes == null && editableTags == null && editableIcons == null) {
            return null;
        }
        return new EditableContent(editableText, editableDetails, editableNote,
            editableAttributes, editableTags, editableIcons);
    }

    private EditableText buildEditableTextForNodeText(NodeModel nodeModel, EditableContentRequest request) {
        Object rawObject = nodeModel.getUserObject();
        boolean includesRaw = request.includesRepresentation(EditableContentRepresentation.RAW);
        boolean includesTransformed = request.includesRepresentation(EditableContentRepresentation.TRANSFORMED);
        boolean includesPlain = request.includesRepresentation(EditableContentRepresentation.PLAIN);
        boolean includesMetadata = request.includesRepresentation(EditableContentRepresentation.METADATA);
        String rawValue = rawObject == null ? null : String.valueOf(rawObject);
        String transformedValue = null;
        if (rawObject != null && (includesTransformed || includesPlain)) {
            Object transformed = textController.getTransformedTextForClipboard(nodeModel, nodeModel, rawObject);
            transformedValue = transformed == null ? null : String.valueOf(transformed);
        }
        String plainValue = null;
        if (includesPlain) {
            plainValue = HtmlUtils.htmlToPlain(transformedValue);
        }
        ContentType contentType = null;
        Boolean hasMarkup = null;
        Boolean isFormula = null;
        if (includesMetadata) {
            boolean formulaDetected = textController.isFormula(rawObject);
            isFormula = formulaDetected;
            hasMarkup = rawValue != null && HtmlUtils.isHtml(rawValue);
            contentType = formulaDetected
                ? ContentType.FORMULA
                : contentTypeConverter.toTextContentTypeForNode(textController.getNodeFormat(nodeModel), rawValue);
        }
        return new EditableText(includesRaw ? rawValue : null,
            includesTransformed ? transformedValue : null,
            plainValue,
            contentType,
            hasMarkup,
            isFormula);
    }

    private EditableText buildEditableText(Object rawObject, String freeplaneContentType, NodeModel nodeModel,
                                           Object nodeProperty, EditableContentRequest request) {
        boolean includesRaw = request.includesRepresentation(EditableContentRepresentation.RAW);
        boolean includesTransformed = request.includesRepresentation(EditableContentRepresentation.TRANSFORMED);
        boolean includesPlain = request.includesRepresentation(EditableContentRepresentation.PLAIN);
        boolean includesMetadata = request.includesRepresentation(EditableContentRepresentation.METADATA);
        String rawValue = rawObject == null ? null : String.valueOf(rawObject);
        String transformedValue = null;
        if (rawObject != null && (includesTransformed || includesPlain)) {
            Object transformed = textController.getTransformedTextForClipboard(nodeModel, nodeProperty, rawObject);
            transformedValue = transformed == null ? null : String.valueOf(transformed);
        }
        String plainValue = null;
        if (includesPlain) {
            plainValue = HtmlUtils.htmlToPlain(transformedValue);
        }
        ContentType contentType = null;
        Boolean hasMarkup = null;
        Boolean isFormula = null;
        if (includesMetadata) {
            boolean formulaDetected = textController.isFormula(rawObject);
            isFormula = formulaDetected;
            hasMarkup = rawValue != null && HtmlUtils.isHtml(rawValue);
            contentType = contentTypeConverter.toContentType(freeplaneContentType, formulaDetected, rawValue);
        }
        return new EditableText(includesRaw ? rawValue : null,
            includesTransformed ? transformedValue : null,
            plainValue,
            contentType,
            hasMarkup,
            isFormula);
    }

    private List<EditableAttribute> buildEditableAttributes(NodeModel nodeModel, EditableContentRequest request) {
        NodeAttributeTableModel attributeTableModel = NodeAttributeTableModel.getModel(nodeModel);
        int rowCount = attributeTableModel.getRowCount();
        if (rowCount == 0) {
            return null;
        }
        boolean includesRaw = request.includesRepresentation(EditableContentRepresentation.RAW);
        boolean includesTransformed = request.includesRepresentation(EditableContentRepresentation.TRANSFORMED);
        boolean includesPlain = request.includesRepresentation(EditableContentRepresentation.PLAIN);
        boolean includesMetadata = request.includesRepresentation(EditableContentRepresentation.METADATA);
        List<EditableAttribute> attributes = new ArrayList<>(rowCount);
        for (int row = 0; row < rowCount; row++) {
            Attribute attribute = attributeTableModel.getAttribute(row);
            if (attribute == null) {
                continue;
            }
            String rawValue = Objects.toString(attribute.getValue(), null);
            String transformedValue = null;
            if (includesTransformed || includesPlain) {
                Object transformed = textController.getTransformedObjectNoFormattingNoThrow(
                    nodeModel, attributeTableModel, rawValue);
                transformedValue = transformed == null ? null : String.valueOf(transformed);
            }
            String plainValue = null;
            if (includesPlain) {
                plainValue = HtmlUtils.htmlToPlain(transformedValue);
            }
            Boolean hasMarkup = null;
            Boolean isFormula = null;
            if (includesMetadata) {
                hasMarkup = rawValue != null && HtmlUtils.isHtml(rawValue);
                isFormula = textController.isFormula(rawValue);
            }
            attributes.add(new EditableAttribute(attribute.getName(),
                includesRaw ? rawValue : null,
                includesTransformed ? transformedValue : null,
                plainValue,
                hasMarkup,
                isFormula,
                row));
        }
        return attributes.isEmpty() ? null : attributes;
    }

    private List<EditableTag> buildEditableTags(NodeModel nodeModel, EditableContentRequest request) {
        List<TagReference> tagReferences = Tags.getTagReferences(nodeModel);
        if (tagReferences == null || tagReferences.isEmpty()) {
            return null;
        }
        boolean includesRaw = request.includesRepresentation(EditableContentRepresentation.RAW)
            || request.includesRepresentation(EditableContentRepresentation.PLAIN)
            || request.includesRepresentation(EditableContentRepresentation.TRANSFORMED);
        if (!includesRaw) {
            return null;
        }
        List<EditableTag> tags = new ArrayList<>(tagReferences.size());
        for (int index = 0; index < tagReferences.size(); index++) {
            TagReference reference = tagReferences.get(index);
            if (reference == null) {
                continue;
            }
            tags.add(new EditableTag(reference.getContent(), index));
        }
        return tags.isEmpty() ? null : tags;
    }

    private List<EditableIcon> buildEditableIcons(NodeModel nodeModel, EditableContentRequest request) {
        List<NamedIcon> icons = nodeModel.getIcons();
        if (icons.isEmpty()) {
            return null;
        }
        boolean includesRaw = request.includesRepresentation(EditableContentRepresentation.RAW)
            || request.includesRepresentation(EditableContentRepresentation.PLAIN)
            || request.includesRepresentation(EditableContentRepresentation.TRANSFORMED);
        if (!includesRaw) {
            return null;
        }
        List<EditableIcon> editableIcons = new ArrayList<>(icons.size());
        for (int index = 0; index < icons.size(); index++) {
            NamedIcon icon = icons.get(index);
            if (icon == null) {
                continue;
            }
            editableIcons.add(new EditableIcon(iconDescriptionResolver.resolveDescription(icon), index));
        }
        return editableIcons.isEmpty() ? null : editableIcons;
    }
}
