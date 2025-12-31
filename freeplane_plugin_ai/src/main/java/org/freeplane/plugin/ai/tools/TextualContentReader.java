package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.text.TextController;

public class TextualContentReader {
    private final TextController textController;

    public TextualContentReader(TextController textController) {
        this.textController = Objects.requireNonNull(textController, "textController");
    }

    public TextualContent readTextualContent(NodeModel nodeModel, NodeContentPreset preset) {
        if (nodeModel == null) {
            return null;
        }
        if (preset == NodeContentPreset.BRIEF) {
            String shortText = textController.getShortPlainText(nodeModel);
            return new TextualContent(shortText, null, null);
        }
        throw new UnsupportedOperationException("Full content is not implemented yet.");
    }
}
