package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import org.freeplane.features.icon.IconController;
import org.freeplane.features.map.NodeModel;

public class TagsContentReader {
    private final IconController iconController;

    public TagsContentReader(IconController iconController) {
        this.iconController = Objects.requireNonNull(iconController, "iconController");
    }

    public TagsContent readTagsContent(NodeModel nodeModel, NodeContentPreset preset) {
        if (nodeModel == null || preset == NodeContentPreset.BRIEF) {
            return null;
        }
        throw new UnsupportedOperationException("Full content is not implemented yet.");
    }
}
