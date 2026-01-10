package org.freeplane.plugin.ai.tools;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.text.DetailModel;

public class TextualContentEditor {
    public void apply(NodeModel nodeModel, TextualContent textualContent) {
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
}
