package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.freeplane.features.text.TextController;
import org.freeplane.plugin.ai.maps.AvailableMaps;
import org.junit.Test;

public class ReadNodeWithContextToolSummaryTest {
    @Test
    public void buildToolCallSummary_usesDefaultDepthsAndSections() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        TextController textController = mock(TextController.class);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(availableMaps, nodeContentItemReader, textController);
        ReadNodesWithContextRequest request = new ReadNodesWithContextRequest(
            "map-identifier",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
        List<ReadNodesWithContextItem> items = Arrays.asList(
            new ReadNodesWithContextItem(Collections.emptyList(), null, null, null),
            new ReadNodesWithContextItem(Collections.emptyList(), null, null, null));
        ReadNodesWithContextResponse response = new ReadNodesWithContextResponse(
            "map-identifier", items, null, Arrays.asList("Alpha", "Beta"));

        ToolCallSummary summary = uut.buildToolCallSummary(request, response);

        assertThat(summary.getToolName()).isEqualTo("readNodeWithContext");
        assertThat(summary.hasError()).isFalse();
        assertThat(summary.getSummaryText()).isEqualTo(
            "readNodeWithContext: items=2, focusNodeTexts=\"Alpha; Beta\"");
    }

    @Test
    public void buildToolCallSummary_includesExplicitSectionsAndDepths() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        TextController textController = mock(TextController.class);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(availableMaps, nodeContentItemReader, textController);
        ReadNodesWithContextRequest request = new ReadNodesWithContextRequest(
            "map-identifier",
            null,
            Arrays.asList(ContextSection.PARENT_SUMMARY, ContextSection.QUALIFIERS),
            2,
            3,
            null,
            null,
            null,
            null);
        ReadNodesWithContextResponse response = new ReadNodesWithContextResponse(
            "map-identifier", Collections.emptyList(), null);

        ToolCallSummary summary = uut.buildToolCallSummary(request, response);

        assertThat(summary.getSummaryText()).isEqualTo(
            "readNodeWithContext: items=0, fullContentDepth=2, summaryDepth=3, "
                + "sections=PARENT_SUMMARY,QUALIFIERS");
    }

    @Test
    public void buildToolCallErrorSummary_sanitizesMessage() {
        AvailableMaps availableMaps = mock(AvailableMaps.class);
        NodeContentItemReader nodeContentItemReader = mock(NodeContentItemReader.class);
        TextController textController = mock(TextController.class);
        ReadNodeWithContextTool uut = new ReadNodeWithContextTool(availableMaps, nodeContentItemReader, textController);

        ToolCallSummary summary = uut.buildToolCallErrorSummary(null, new IllegalStateException("Missing\nmap"));

        assertThat(summary.getToolName()).isEqualTo("readNodeWithContext");
        assertThat(summary.hasError()).isTrue();
        assertThat(summary.getSummaryText()).isEqualTo("readNodeWithContext error: Missing map");
    }
}
