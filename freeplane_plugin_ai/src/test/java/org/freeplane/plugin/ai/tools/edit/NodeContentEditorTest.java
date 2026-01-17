package org.freeplane.plugin.ai.tools.edit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.tools.content.ContentType;
import org.freeplane.plugin.ai.tools.content.NodeContentItem;
import org.freeplane.plugin.ai.tools.content.NodeContentItemReader;
import org.freeplane.plugin.ai.tools.content.NodeContentPreset;
import org.freeplane.plugin.ai.tools.content.NodeContentResponse;
import org.junit.Test;

public class NodeContentEditorTest {
    @Test
    public void edit_returnsFullContentWhenNoEdits() {
        TextController textController = mock(TextController.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        TextualContentEditor textualContentEditor = mock(TextualContentEditor.class);
        AttributesContentEditor attributesContentEditor = mock(AttributesContentEditor.class);
        TagsContentEditor tagsContentEditor = mock(TagsContentEditor.class);
        IconsContentEditor iconsContentEditor = mock(IconsContentEditor.class);
        NodeContentEditor editor = new NodeContentEditor(textController, nodeContentItemReader, textualContentEditor,
            attributesContentEditor, tagsContentEditor, iconsContentEditor);
        NodeModel nodeModel = mock(NodeModel.class);
        NodeContentItem contentItem = new NodeContentItem("node-identifier",
            new NodeContentResponse("text", null, null, null, null, null), Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(nodeModel, NodeContentPreset.FULL)).thenReturn(contentItem);

        NodeContentItem result = editor.edit(nodeModel, Collections.emptyList());

        assertThat(result).isSameAs(contentItem);
        verify(nodeContentItemReader).readNodeContentItem(nodeModel, NodeContentPreset.FULL);
        verifyNoInteractions(textualContentEditor, attributesContentEditor, tagsContentEditor, iconsContentEditor);
    }

    @Test
    public void edit_throwsWhenOperationIsNotReplaceForText() {
        TextController textController = mock(TextController.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        TextualContentEditor textualContentEditor = mock(TextualContentEditor.class);
        AttributesContentEditor attributesContentEditor = mock(AttributesContentEditor.class);
        TagsContentEditor tagsContentEditor = mock(TagsContentEditor.class);
        IconsContentEditor iconsContentEditor = mock(IconsContentEditor.class);
        NodeContentEditor editor = new NodeContentEditor(textController, nodeContentItemReader, textualContentEditor,
            attributesContentEditor, tagsContentEditor, iconsContentEditor);
        NodeModel nodeModel = mock(NodeModel.class);
        NodeContentEditItem editItem = new NodeContentEditItem(
            "node-identifier", EditedElement.TEXT, ContentType.PLAIN_TEXT, "updated", null, EditOperation.ADD, null);

        assertThatThrownBy(() -> editor.edit(nodeModel, Collections.singletonList(editItem)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Only REPLACE operations are supported for this element.");
    }

    @Test
    public void edit_delegatesTextEditsToTextualContentEditor() {
        TextController textController = mock(TextController.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        TextualContentEditor textualContentEditor = mock(TextualContentEditor.class);
        AttributesContentEditor attributesContentEditor = mock(AttributesContentEditor.class);
        TagsContentEditor tagsContentEditor = mock(TagsContentEditor.class);
        IconsContentEditor iconsContentEditor = mock(IconsContentEditor.class);
        NodeContentEditor editor = new NodeContentEditor(textController, nodeContentItemReader, textualContentEditor,
            attributesContentEditor, tagsContentEditor, iconsContentEditor);
        NodeModel nodeModel = mock(NodeModel.class);
        NodeContentEditItem editItem = new NodeContentEditItem(
            "node-identifier", EditedElement.TEXT, ContentType.PLAIN_TEXT, "updated", null, EditOperation.REPLACE, null);
        NodeContentItem contentItem = new NodeContentItem("node-identifier",
            new NodeContentResponse("updated", null, null, null, null, null), Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(nodeModel, NodeContentPreset.FULL)).thenReturn(contentItem);

        NodeContentItem result = editor.edit(nodeModel, Collections.singletonList(editItem));

        assertThat(result).isSameAs(contentItem);
        verify(textualContentEditor).editExistingTextualContent(
            nodeModel, EditedElement.TEXT, ContentType.PLAIN_TEXT, "updated", textController);
    }
}
