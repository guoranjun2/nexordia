package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;
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
        NodeContentEditor uut = new NodeContentEditor(textController, nodeContentItemReader, textualContentEditor,
            attributesContentEditor, tagsContentEditor, iconsContentEditor);
        NodeModel nodeModel = mock(NodeModel.class);
        NodeContentItem contentItem = new NodeContentItem("node-identifier",
            new NodeContent("text", null, null, null, null), Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(nodeModel, NodeContentPreset.FULL)).thenReturn(contentItem);
        NodeContentEditRequest request = new NodeContentEditRequest("map-identifier", "node-identifier", null,
            Collections.emptyList());

        NodeContentItem result = uut.edit(nodeModel, request);

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
        NodeContentEditor uut = new NodeContentEditor(textController, nodeContentItemReader, textualContentEditor,
            attributesContentEditor, tagsContentEditor, iconsContentEditor);
        NodeModel nodeModel = mock(NodeModel.class);
        NodeContentEditItem editItem = new NodeContentEditItem(
            EditedElement.TEXT, ContentType.PLAIN_TEXT, "updated", null, EditOperation.ADD, null);
        NodeContentEditRequest request = new NodeContentEditRequest("map-identifier", "node-identifier", null,
            Collections.singletonList(editItem));

        assertThatThrownBy(() -> uut.edit(nodeModel, request))
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
        NodeContentEditor uut = new NodeContentEditor(textController, nodeContentItemReader, textualContentEditor,
            attributesContentEditor, tagsContentEditor, iconsContentEditor);
        NodeModel nodeModel = mock(NodeModel.class);
        NodeContentEditItem editItem = new NodeContentEditItem(
            EditedElement.TEXT, ContentType.PLAIN_TEXT, "updated", null, EditOperation.REPLACE, null);
        NodeContentEditRequest request = new NodeContentEditRequest("map-identifier", "node-identifier", null,
            Collections.singletonList(editItem));
        NodeContentItem contentItem = new NodeContentItem("node-identifier",
            new NodeContent("updated", null, null, null, null), Collections.emptyList());
        when(nodeContentItemReader.readNodeContentItem(nodeModel, NodeContentPreset.FULL)).thenReturn(contentItem);

        NodeContentItem result = uut.edit(nodeModel, request);

        assertThat(result).isSameAs(contentItem);
        verify(textualContentEditor).editExistingTextualContent(
            nodeModel, EditedElement.TEXT, ContentType.PLAIN_TEXT, "updated", textController);
    }
}
