package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.note.NoteModel;
import org.freeplane.features.text.DetailModel;
import org.junit.Test;

public class TextualContentEditorTest {
    @Test
    public void apply_setsTextDetailsAndNote() {
        MapModel mapModel = new MapModel((source, targetMap, withChildren) -> null, null, null);
        NodeModel nodeModel = new NodeModel("node", mapModel);
        TextualContent textualContent = new TextualContent("text", "details", "note");
        TextualContentEditor unitUnderTest = new TextualContentEditor();

        unitUnderTest.apply(nodeModel, textualContent);

        assertThat(nodeModel.getText()).isEqualTo("text");
        assertThat(HtmlUtils.htmlToPlain(DetailModel.getDetailText(nodeModel))).isEqualTo("details");
        assertThat(HtmlUtils.htmlToPlain(NoteModel.getNoteText(nodeModel))).isEqualTo("note");
    }
}
