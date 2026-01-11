package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.TextController;

public class TextualContentEditor {
    private final TextContentWriteController textContentWriteController;
    private final NoteContentWriteController noteContentWriteController;
    private final ContentTypeConverter contentTypeConverter;

    public TextualContentEditor(TextContentWriteController textContentWriteController,
                                NoteContentWriteController noteContentWriteController) {
        this.textContentWriteController = Objects.requireNonNull(
            textContentWriteController, "textContentWriteController");
        this.noteContentWriteController = Objects.requireNonNull(
            noteContentWriteController, "noteContentWriteController");
        this.contentTypeConverter = new ContentTypeConverter();
    }

    public void setInitialContent(NodeModel nodeModel, TextualContent textualContent) {
        if (nodeModel == null || textualContent == null) {
            return;
        }
        if (textualContent.getText() != null) {
            nodeModel.setText(textualContent.getText());
        }
        String detailsContent = textualContent.getDetails();
		if (detailsContent != null && ! detailsContent.isEmpty()) {
            DetailModel details = DetailModel.createDetailText(nodeModel);
            details.setText(htmlOf(detailsContent)) ;
        }
        String noteContent = textualContent.getNote();
		if (noteContent != null && ! noteContent.isEmpty()) {
            NoteModel note = NoteModel.createNote(nodeModel);
            note.setText(htmlOf(noteContent));
        }
    }

    private String htmlOf(String text) {
        return HtmlUtils.isHtml(text) ? text : HtmlUtils.plainToHTML(text);
    }

    public void editExistingTextualContent(NodeModel nodeModel, EditedElement editedElement,
                                           ContentType originalContentType, String value,
                                           TextController textController) {
        if (nodeModel == null) {
            throw new IllegalArgumentException("Missing node model.");
        }
        if (editedElement == null) {
            throw new IllegalArgumentException("Missing edited element.");
        }
        if (textController == null) {
            throw new IllegalArgumentException("Missing text controller.");
        }
        switch (editedElement) {
            case TEXT:
                Object currentTextValue = nodeModel.getUserObject();
                ensureFormulaIsNotUsed(currentTextValue, value, originalContentType, textController);
                NodeTextContentType nodeTextContentType = resolveNodeTextContentType(
                    nodeModel, currentTextValue, textController);
                validateContentType(nodeTextContentType.contentType, originalContentType);
                String updatedTextValue = prepareTextValue(nodeTextContentType, value);
                textContentWriteController.setNodeText(nodeModel, updatedTextValue);
                break;
            case DETAILS:
                DetailModel detailModel = DetailModel.getDetail(nodeModel);
                String currentDetails = detailModel == null ? null : detailModel.getText();
                ensureFormulaIsNotUsed(currentDetails, value, originalContentType, textController);
                ContentType currentDetailsContentType = resolveContentType(
                    currentDetails, DetailModel.getDetailContentType(nodeModel), textController);
                validateContentType(currentDetailsContentType, originalContentType);
                String updatedDetailsValue = prepareRichTextValue(currentDetailsContentType, value, editedElement);
                textContentWriteController.setDetails(nodeModel, updatedDetailsValue);
                break;
            case NOTE:
                NoteModel noteModel = NoteModel.getNote(nodeModel);
                String currentNote = noteModel == null ? null : noteModel.getText();
                ensureFormulaIsNotUsed(currentNote, value, originalContentType, textController);
                ContentType currentNoteContentType = resolveContentType(
                    currentNote, NoteModel.getNoteContentType(nodeModel), textController);
                validateContentType(currentNoteContentType, originalContentType);
                String updatedNoteValue = prepareRichTextValue(currentNoteContentType, value, editedElement);
                noteContentWriteController.setNoteText(nodeModel, updatedNoteValue);
                break;
            default:
                throw new IllegalArgumentException("Unsupported edited element for textual content: " + editedElement);
        }
    }

    private void ensureFormulaIsNotUsed(Object currentValue, String newValue, ContentType originalContentType,
                                        TextController textController) {
        if (originalContentType == ContentType.FORMULA) {
            throw new IllegalArgumentException("Formula content edits are not allowed.");
        }
        if (textController.isFormula(currentValue)) {
            throw new IllegalArgumentException("Cannot edit formula content.");
        }
        if (textController.isFormula(newValue)) {
            throw new IllegalArgumentException("Formula content edits are not allowed.");
        }
    }

    private void validateContentType(ContentType currentContentType, ContentType originalContentType) {
        if (originalContentType == null) {
            return;
        }
        if (currentContentType != originalContentType) {
            throw new IllegalArgumentException("Content type has changed; read editable content again.");
        }
    }

    private ContentType resolveContentType(Object currentValue, String freeplaneContentType,
                                           TextController textController) {
        boolean isFormula = textController.isFormula(currentValue);
        return contentTypeConverter.toContentType(
            freeplaneContentType, isFormula, currentValue == null ? null : String.valueOf(currentValue));
    }

    private NodeTextContentType resolveNodeTextContentType(NodeModel nodeModel, Object currentValue,
                                                          TextController textController) {
        String rawValue = currentValue == null ? null : String.valueOf(currentValue);
        String latexPrefix = contentTypeConverter.findLatexPrefix(rawValue);
        ContentType contentType = latexPrefix == null
            ? contentTypeConverter.toTextContentTypeForNode(textController.getNodeFormat(nodeModel), rawValue)
            : ContentType.LATEX;
        return new NodeTextContentType(contentType, latexPrefix);
    }

    private String prepareTextValue(NodeTextContentType nodeTextContentType, String value) {
        if (nodeTextContentType.contentType == ContentType.LATEX) {
            boolean allowLatexPrefixWithoutReapply = false;
            return prepareLatexValue(value, nodeTextContentType.latexPrefix, allowLatexPrefixWithoutReapply);
        }
        if (nodeTextContentType.contentType == ContentType.MARKDOWN) {
            rejectHtml(value, "Markdown text content does not allow html.");
        }
        return value;
    }

    private String prepareRichTextValue(ContentType currentContentType, String value, EditedElement editedElement) {
        if (currentContentType == ContentType.LATEX) {
            boolean allowLatexPrefixWithoutReapply = true;
            return prepareLatexValue(value, null, allowLatexPrefixWithoutReapply);
        }
        if (currentContentType == ContentType.MARKDOWN) {
            rejectHtml(value, "Markdown content does not allow html for " + editedElement + ".");
        }
        return value;
    }

    private String prepareLatexValue(String value, String latexPrefix, boolean allowLatexPrefixWithoutReapply) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing latex content.");
        }
        rejectHtml(value, "Latex content does not allow html.");
        if (latexPrefix == null && contentTypeConverter.findLatexPrefix(value) != null
            && !allowLatexPrefixWithoutReapply) {
            throw new IllegalArgumentException("Latex prefix is not allowed for this content.");
        }
        String strippedValue = contentTypeConverter.stripLatexPrefix(value);
        if (latexPrefix == null) {
            return strippedValue;
        }
        return latexPrefix + " " + strippedValue;
    }

    private void rejectHtml(String value, String message) {
        if (value != null && HtmlUtils.isHtml(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private static class NodeTextContentType {
        private final ContentType contentType;
        private final String latexPrefix;

        private NodeTextContentType(ContentType contentType, String latexPrefix) {
            this.contentType = contentType;
            this.latexPrefix = latexPrefix;
        }
    }
}
