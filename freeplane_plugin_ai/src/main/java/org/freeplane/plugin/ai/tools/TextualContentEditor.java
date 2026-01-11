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

    public TextualContentEditor(TextContentWriteController textContentWriteController,
                                NoteContentWriteController noteContentWriteController) {
        this.textContentWriteController = Objects.requireNonNull(
            textContentWriteController, "textContentWriteController");
        this.noteContentWriteController = Objects.requireNonNull(
            noteContentWriteController, "noteContentWriteController");
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

    public void editExistingTextualContent(NodeModel nodeModel, EditedElement editedElement, ContentType contentType,
                                           String value, TextController textController) {
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
                ensureFormulaIsNotUsed(nodeModel.getUserObject(), value, contentType, textController);
                textContentWriteController.setNodeText(nodeModel, value);
                break;
            case DETAILS:
                DetailModel detailModel = DetailModel.getDetail(nodeModel);
                String currentDetails = detailModel == null ? null : detailModel.getText();
                ensureFormulaIsNotUsed(currentDetails, value, contentType, textController);
                textContentWriteController.setDetails(nodeModel, value);
                break;
            case NOTE:
                NoteModel noteModel = NoteModel.getNote(nodeModel);
                String currentNote = noteModel == null ? null : noteModel.getText();
                ensureFormulaIsNotUsed(currentNote, value, contentType, textController);
                noteContentWriteController.setNoteText(nodeModel, value);
                break;
            default:
                throw new IllegalArgumentException("Unsupported edited element for textual content: " + editedElement);
        }
    }

    private void ensureFormulaIsNotUsed(Object currentValue, String newValue, ContentType contentType,
                                        TextController textController) {
        if (contentType == ContentType.FORMULA) {
            throw new IllegalArgumentException("Formula content edits are not allowed.");
        }
        if (textController.isFormula(currentValue)) {
            throw new IllegalArgumentException("Cannot edit formula content.");
        }
        if (textController.isFormula(newValue)) {
            throw new IllegalArgumentException("Formula content edits are not allowed.");
        }
    }
}
