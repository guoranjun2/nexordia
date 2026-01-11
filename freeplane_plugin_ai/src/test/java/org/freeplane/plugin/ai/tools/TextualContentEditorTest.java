package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.TextController;
import org.junit.Test;

public class TextualContentEditorTest {
    @Test
    public void setInitialContent_setsTextDetailsAndNote() {
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel nodeModel = new NodeModel("node", mapModel);
        TextualContent textualContent = new TextualContent("text", "details", "note");
        TextualContentEditor uut = new TextualContentEditor(
            mock(TextContentWriteController.class), mock(NoteContentWriteController.class));

        uut.setInitialContent(nodeModel, textualContent);

        assertThat(nodeModel.getText()).isEqualTo("text");
        assertThat(HtmlUtils.htmlToPlain(DetailModel.getDetailText(nodeModel))).isEqualTo("details");
        assertThat(HtmlUtils.htmlToPlain(NoteModel.getNoteText(nodeModel))).isEqualTo("note");
    }

    @Test
    public void editExistingTextualContent_updatesNodeTextThroughController() {
        TextContentWriteController textContentWriteController = mock(TextContentWriteController.class);
        NoteContentWriteController noteContentWriteController = mock(NoteContentWriteController.class);
        TextualContentEditor uut = new TextualContentEditor(textContentWriteController, noteContentWriteController);
        NodeModel nodeModel = mock(NodeModel.class);
        TextController textController = mock(TextController.class);

        uut.editExistingTextualContent(nodeModel, EditedElement.TEXT, ContentType.PLAIN_TEXT, "value", textController);

        verify(textContentWriteController).setNodeText(nodeModel, "value");
    }

    @Test
    public void editExistingTextualContent_throwsOnFormulaContentType() {
        TextualContentEditor uut = new TextualContentEditor(
            mock(TextContentWriteController.class), mock(NoteContentWriteController.class));
        NodeModel nodeModel = mock(NodeModel.class);
        TextController textController = mock(TextController.class);

        assertThatThrownBy(() -> uut.editExistingTextualContent(
            nodeModel, EditedElement.TEXT, ContentType.FORMULA, "value", textController))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Formula content edits are not allowed.");
    }
}
