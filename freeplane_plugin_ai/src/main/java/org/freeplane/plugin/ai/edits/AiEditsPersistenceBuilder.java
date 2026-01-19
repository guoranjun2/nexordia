package org.freeplane.plugin.ai.edits;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.IAttributeHandler;
import org.freeplane.core.io.IExtensionAttributeWriter;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.core.io.ReadManager;
import org.freeplane.core.io.WriteManager;
import org.freeplane.features.map.NodeBuilder;
import org.freeplane.features.map.NodeModel;

public class AiEditsPersistenceBuilder implements IExtensionAttributeWriter {
    static final String AI_EDITS_ATTRIBUTE = "AI_EDITS";

    private final AiEditsSettings aiEditsSettings;
    private final IAttributeHandler attributeHandler = new IAttributeHandler() {
        @Override
        public void setAttribute(final Object userObject, final String value) {
            NodeModel node = (NodeModel) userObject;
            if (node.getExtension(AIEdits.class) == null) {
                node.addExtension(new AIEdits());
            }
        }
    };

    public AiEditsPersistenceBuilder(AiEditsSettings aiEditsSettings) {
        this.aiEditsSettings = aiEditsSettings;
    }

    public void registerBy(ReadManager readManager, WriteManager writeManager) {
        readManager.addAttributeHandler(NodeBuilder.XML_NODE, AI_EDITS_ATTRIBUTE, attributeHandler);
        readManager.addAttributeHandler(NodeBuilder.XML_STYLENODE, AI_EDITS_ATTRIBUTE, attributeHandler);
        writeManager.addExtensionAttributeWriter(AIEdits.class, this);
    }

    @Override
    public void writeAttributes(final ITreeWriter writer, final Object userObject, final IExtension extension) {
        if (!aiEditsSettings.isPersistenceEnabled()) {
            return;
        }
        writer.addAttribute(AI_EDITS_ATTRIBUTE, "true");
    }
}
