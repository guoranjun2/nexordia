package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
}
