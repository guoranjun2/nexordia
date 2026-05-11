package org.freeplane.plugin.ai.prompt;

import java.util.List;
import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.menubuilders.generic.Entry;
import org.freeplane.core.ui.menubuilders.generic.EntryAccessor;
import org.freeplane.core.ui.menubuilders.generic.EntryVisitor;
import org.freeplane.features.mode.ModeController;

class AiPromptMenuBuilder implements EntryVisitor {
    private final ModeController modeController;
    private final AiPromptActionRegistry promptActionRegistry;

    AiPromptMenuBuilder(ModeController modeController, AiPromptActionRegistry promptActionRegistry) {
        this.modeController = modeController;
        this.promptActionRegistry = promptActionRegistry;
    }

    @Override
    public void visit(Entry target) {
        List<RunAiPromptAction> promptActions = promptActionRegistry.promptActions();
        if (promptActions.isEmpty()) {
            return;
        }
        target.addChild(new Entry().setBuilders("separator"));
        EntryAccessor entryAccessor = new EntryAccessor();
        for (RunAiPromptAction promptAction : promptActions) {
            AFreeplaneAction action = modeController.addActionIfNotAlreadySet(promptAction);
            entryAccessor.addChildAction(target, action);
        }
    }

    @Override
    public boolean shouldSkipChildren(Entry entry) {
        return true;
    }
}
