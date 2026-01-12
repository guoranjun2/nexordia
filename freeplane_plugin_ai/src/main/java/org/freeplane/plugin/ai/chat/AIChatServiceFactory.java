package org.freeplane.plugin.ai.chat;

import org.freeplane.plugin.ai.tools.AIToolSet;
import org.freeplane.plugin.ai.tools.ToolCallSummaryHandler;

import dev.langchain4j.model.chat.ChatModel;

public class AIChatServiceFactory {

    private AIChatServiceFactory() {
    }

    public static AIChatService createService(AIToolSet toolSet, ChatSessionMemoryController chatSessionMemoryController,
                                              ChatTokenUsageTracker chatTokenUsageTracker,
                                              ToolCallSummaryHandler toolCallSummaryHandler) {
        AIProviderConfiguration configuration = new AIProviderConfiguration();
        ChatModel chatLanguageModel = AIChatModelFactory.createChatLanguageModel(configuration);
        return new AIChatService(chatLanguageModel, toolSet, chatSessionMemoryController.getChatMemory(),
            chatTokenUsageTracker, toolCallSummaryHandler);
    }
}
