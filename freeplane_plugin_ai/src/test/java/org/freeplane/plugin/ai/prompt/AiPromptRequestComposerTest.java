package org.freeplane.plugin.ai.prompt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.freeplane.plugin.ai.tools.selection.SelectedNodeSummary;
import org.freeplane.plugin.ai.tools.selection.SelectionIdentifiersResponse;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AiPromptRequestComposerTest {

    @Test
    public void compose_prependsSelectedIdentifiersJsonAndPromptText() {
        SelectionIdentifiersResponse response = new SelectionIdentifiersResponse(
            "map-1",
            "node-1",
            "root-1",
            Arrays.asList(
                new SelectedNodeSummary("node-1", "Alpha"),
                new SelectedNodeSummary("node-2", "Beta")),
            2,
            1);
        AiPromptRequestComposer uut = new AiPromptRequestComposer(() -> response, new ObjectMapper());

        String composed = uut.compose(new AiPrompt("Rewrite", "Rewrite the selected nodes.", false));

        assertThat(composed).isEqualTo(
            "Selected map and node identifiers:\n"
                + "{\"mapIdentifier\":\"map-1\",\"nodeIdentifier\":\"node-1\","
                + "\"rootNodeIdentifier\":\"root-1\",\"selectedNodes\":[{\"nodeIdentifier\":\"node-1\",\"shortText\":\"Alpha\"},"
                + "{\"nodeIdentifier\":\"node-2\",\"shortText\":\"Beta\"}],\"selectedNodeCount\":2,\"selectedUniqueSubtreeCount\":1}"
                + "\n\nRewrite the selected nodes.");
    }
}
