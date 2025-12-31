package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.freeplane.features.map.NodeModel;
import org.junit.Test;

public class NodeContentReaderTest {
    @Test
    public void readNodeContent_returnsBriefTextOnlyForBriefPreset() {
        TextualContentReader textualContentReader = mock(TextualContentReader.class);
        AttributesContentReader attributesContentReader = mock(AttributesContentReader.class);
        TagsContentReader tagsContentReader = mock(TagsContentReader.class);
        NodeModel nodeModel = mock(NodeModel.class);
        when(textualContentReader.readBriefText(nodeModel)).thenReturn("Brief text");
        NodeContentReader uut = new NodeContentReader(
            textualContentReader, attributesContentReader, tagsContentReader);

        NodeContent content = uut.readNodeContent(nodeModel, NodeContentPreset.BRIEF);

        assertThat(content.getBriefText()).isEqualTo("Brief text");
        assertThat(content.getTextualContent()).isNull();
        assertThat(content.getAttributesContent()).isNull();
        assertThat(content.getTagsContent()).isNull();
        verify(textualContentReader).readBriefText(nodeModel);
        verifyNoInteractions(attributesContentReader, tagsContentReader);
    }

    @Test
    public void readNodeContent_returnsFullContentForFullPreset() {
        TextualContentReader textualContentReader = mock(TextualContentReader.class);
        AttributesContentReader attributesContentReader = mock(AttributesContentReader.class);
        TagsContentReader tagsContentReader = mock(TagsContentReader.class);
        NodeModel nodeModel = mock(NodeModel.class);
        TextualContent textualContent = new TextualContent("Text", "Details", "Note");
        AttributesContent attributesContent = new AttributesContent(Collections.emptyList());
        TagsContent tagsContent = new TagsContent(Collections.emptyList());
        when(textualContentReader.readTextualContent(nodeModel, NodeContentPreset.FULL)).thenReturn(textualContent);
        when(attributesContentReader.readAttributesContent(nodeModel, NodeContentPreset.FULL)).thenReturn(attributesContent);
        when(tagsContentReader.readTagsContent(nodeModel, NodeContentPreset.FULL)).thenReturn(tagsContent);
        NodeContentReader uut = new NodeContentReader(
            textualContentReader, attributesContentReader, tagsContentReader);

        NodeContent content = uut.readNodeContent(nodeModel, NodeContentPreset.FULL);

        assertThat(content.getBriefText()).isNull();
        assertThat(content.getTextualContent()).isSameAs(textualContent);
        assertThat(content.getAttributesContent()).isSameAs(attributesContent);
        assertThat(content.getTagsContent()).isSameAs(tagsContent);
        verify(textualContentReader).readTextualContent(nodeModel, NodeContentPreset.FULL);
        verify(attributesContentReader).readAttributesContent(nodeModel, NodeContentPreset.FULL);
        verify(tagsContentReader).readTagsContent(nodeModel, NodeContentPreset.FULL);
    }
}
