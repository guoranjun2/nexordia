package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.junit.Test;

public class TextualContentReaderTest {
    @Test
    public void readTextualContent_usesShortPlainTextForBrief() {
        TextController textController = mock(TextController.class);
        NodeModel nodeModel = mock(NodeModel.class);
        when(textController.getShortPlainText(nodeModel)).thenReturn("Short text");
        TextualContentReader uut = new TextualContentReader(textController);

        TextualContent content = uut.readTextualContent(nodeModel, NodeContentPreset.BRIEF);

        assertThat(content.getText()).isEqualTo("Short text");
        verify(textController).getShortPlainText(nodeModel);
    }
}
