package org.freeplane.plugin.ai.chat;

import dev.langchain4j.model.chat.ChatModel;
import org.freeplane.plugin.ai.tools.AIToolSet;

public class AIChatServiceFactory {

    private AIChatServiceFactory() {
    }

    public static AIChatService createService(AIToolSet toolSet, ChatSessionMemoryController chatSessionMemoryController,
                                              ChatTokenUsageTracker chatTokenUsageTracker) {
        AIProviderConfiguration configuration = new AIProviderConfiguration();
        ChatModel chatLanguageModel = AIChatModelFactory.createChatLanguageModel(configuration);
        return new AIChatService(chatLanguageModel, toolSet, chatSessionMemoryController.getChatMemory(),
            chatTokenUsageTracker);
    }
}
