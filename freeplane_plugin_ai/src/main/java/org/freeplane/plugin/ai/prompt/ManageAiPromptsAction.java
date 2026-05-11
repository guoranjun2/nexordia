package org.freeplane.plugin.ai.prompt;

import java.awt.event.ActionEvent;
import org.freeplane.core.ui.AFreeplaneAction;

class ManageAiPromptsAction extends AFreeplaneAction {
    private static final long serialVersionUID = 1L;

    private final AiPromptActionRegistry promptActionRegistry;

    ManageAiPromptsAction(AiPromptActionRegistry promptActionRegistry) {
        super("ManageAiPromptsAction");
        this.promptActionRegistry = promptActionRegistry;
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        promptActionRegistry.openPromptManager();
    }
}
