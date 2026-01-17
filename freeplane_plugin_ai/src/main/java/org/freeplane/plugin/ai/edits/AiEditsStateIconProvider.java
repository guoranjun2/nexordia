package org.freeplane.plugin.ai.edits;

import org.freeplane.features.icon.IStateIconProvider;
import org.freeplane.features.icon.UIIcon;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.features.map.NodeModel;

public class AiEditsStateIconProvider implements IStateIconProvider {
    private final AiEditsSettings aiEditsSettings;
    private final UIIcon aiEditsIcon;

    public AiEditsStateIconProvider(AiEditsSettings aiEditsSettings) {
        this.aiEditsSettings = aiEditsSettings;
        this.aiEditsIcon = IconStoreFactory.ICON_STORE.getUIIcon("ai.svg");
    }

    @Override
    public UIIcon getStateIcon(NodeModel node) {
        if (!aiEditsSettings.isStateIconVisible()) {
            return null;
        }
        if (node.getExtension(AIEdits.class) == null) {
            return null;
        }
        return aiEditsIcon;
    }

    @Override
    public boolean mustIncludeInIconRegistry() {
        return true;
    }
}
