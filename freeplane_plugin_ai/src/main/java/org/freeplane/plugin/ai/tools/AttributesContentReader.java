package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import org.freeplane.features.attribute.AttributeController;
import org.freeplane.features.map.NodeModel;

public class AttributesContentReader {
    private final AttributeController attributeController;

    public AttributesContentReader(AttributeController attributeController) {
        this.attributeController = Objects.requireNonNull(attributeController, "attributeController");
    }

    public AttributesContent readAttributesContent(NodeModel nodeModel, NodeContentPreset preset) {
        if (nodeModel == null || preset == NodeContentPreset.BRIEF) {
            return null;
        }
        throw new UnsupportedOperationException("Full content is not implemented yet.");
    }
}
