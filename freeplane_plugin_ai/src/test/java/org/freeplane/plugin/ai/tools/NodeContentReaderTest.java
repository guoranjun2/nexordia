package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
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
        IconsContentReader iconsContentReader = mock(IconsContentReader.class);
        NodeModel nodeModel = mock(NodeModel.class);
        when(textualContentReader.readBriefText(nodeModel)).thenReturn("Brief text");
        NodeContentReader nodeContentReader = new NodeContentReader(
            textualContentReader, attributesContentReader, tagsContentReader, iconsContentReader);

        NodeContent content = nodeContentReader.readNodeContent(nodeModel, NodeContentPreset.BRIEF);

        assertThat(content.getBriefText()).isEqualTo("Brief text");
        assertThat(content.getTextualContent()).isNull();
        assertThat(content.getAttributesContent()).isNull();
        assertThat(content.getTagsContent()).isNull();
        assertThat(content.getIconsContent()).isNull();
        verify(textualContentReader).readBriefText(nodeModel);
        verifyNoInteractions(attributesContentReader, tagsContentReader, iconsContentReader);
    }

    @Test
    public void readNodeContent_returnsFullContentForFullPreset() {
        TextualContentReader textualContentReader = mock(TextualContentReader.class);
        AttributesContentReader attributesContentReader = mock(AttributesContentReader.class);
        TagsContentReader tagsContentReader = mock(TagsContentReader.class);
        IconsContentReader iconsContentReader = mock(IconsContentReader.class);
        NodeModel nodeModel = mock(NodeModel.class);
        TextualContent textualContent = new TextualContent("Text", "Details", "Note");
        AttributesContent attributesContent = new AttributesContent(Collections.emptyList());
        TagsContent tagsContent = new TagsContent(Collections.emptyList());
        IconsContent iconsContent = new IconsContent(Collections.singletonList("Icon"));
        when(textualContentReader.readTextualContent(nodeModel, NodeContentPreset.FULL)).thenReturn(textualContent);
        when(attributesContentReader.readAttributesContent(nodeModel, NodeContentPreset.FULL)).thenReturn(attributesContent);
        when(tagsContentReader.readTagsContent(nodeModel, NodeContentPreset.FULL)).thenReturn(tagsContent);
        when(iconsContentReader.readIconsContent(nodeModel, NodeContentPreset.FULL)).thenReturn(iconsContent);
        NodeContentReader nodeContentReader = new NodeContentReader(
            textualContentReader, attributesContentReader, tagsContentReader, iconsContentReader);

        NodeContent content = nodeContentReader.readNodeContent(nodeModel, NodeContentPreset.FULL);

        assertThat(content.getBriefText()).isNull();
        assertThat(content.getTextualContent()).isSameAs(textualContent);
        assertThat(content.getAttributesContent()).isSameAs(attributesContent);
        assertThat(content.getTagsContent()).isSameAs(tagsContent);
        assertThat(content.getIconsContent()).isSameAs(iconsContent);
        verify(textualContentReader).readTextualContent(nodeModel, NodeContentPreset.FULL);
        verify(attributesContentReader).readAttributesContent(nodeModel, NodeContentPreset.FULL);
        verify(tagsContentReader).readTagsContent(nodeModel, NodeContentPreset.FULL);
        verify(iconsContentReader).readIconsContent(nodeModel, NodeContentPreset.FULL);
    }

    @Test
    public void matches_returnsTrueWhenIconSearchTermsMatch() {
        TextualContentReader textualContentReader = mock(TextualContentReader.class);
        AttributesContentReader attributesContentReader = mock(AttributesContentReader.class);
        TagsContentReader tagsContentReader = mock(TagsContentReader.class);
        IconsContentReader iconsContentReader = mock(IconsContentReader.class);
        NodeContentReader nodeContentReader = new NodeContentReader(
            textualContentReader, attributesContentReader, tagsContentReader, iconsContentReader);
        NodeModel nodeModel = mock(NodeModel.class);
        IconsContentRequest iconsContentRequest = new IconsContentRequest(true);
        NodeContentRequest request = new NodeContentRequest(null, null, null, iconsContentRequest);
        when(iconsContentReader.matches(eq(nodeModel), eq(iconsContentRequest), any(NodeContentValueMatcher.class)))
            .thenAnswer(invocation -> {
                NodeContentValueMatcher matcher = invocation.getArgument(2);
                return matcher.matchesValue("Priority 3");
            });
        NodeContentValueMatcher valueMatcher = new NodeContentValueMatcher(
            "priority", SearchMatchingMode.CONTAINS, SearchCaseSensitivity.CASE_INSENSITIVE, null);

        boolean matched = nodeContentReader.matches(nodeModel, request, valueMatcher);

        assertThat(matched).isTrue();
    }
}
