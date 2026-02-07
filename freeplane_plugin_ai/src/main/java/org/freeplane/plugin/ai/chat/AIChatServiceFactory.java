package org.freeplane.plugin.ai.chat;

import org.freeplane.plugin.ai.tools.AIToolSet;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummaryHandler;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.memory.ChatMemory;
import java.util.function.Supplier;

public class AIChatServiceFactory {

    private AIChatServiceFactory() {
    }

    public static AIChatService createService(AIToolSet toolSet, ChatMemory chatMemory,
                                              ChatTokenUsageTracker chatTokenUsageTracker,
                                              ToolCallSummaryHandler toolCallSummaryHandler) {
        return createService(toolSet, chatMemory, chatTokenUsageTracker, toolCallSummaryHandler, null);
    }

    public static AIChatService createService(AIToolSet toolSet, ChatMemory chatMemory,
                                              ChatTokenUsageTracker chatTokenUsageTracker,
                                              ToolCallSummaryHandler toolCallSummaryHandler,
                                              Supplier<Boolean> cancellationSupplier) {
        AIProviderConfiguration configuration = new AIProviderConfiguration();
        ChatModel chatLanguageModel = AIChatModelFactory.createChatLanguageModel(configuration);
        return new AIChatService(chatLanguageModel, toolSet, chatMemory,
            chatTokenUsageTracker, toolCallSummaryHandler, cancellationSupplier);
    }
}
