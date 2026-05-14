package org.freeplane.plugin.ai.chat;

import org.freeplane.plugin.ai.model.AIChatModelFactory;
import org.freeplane.plugin.ai.model.AIProviderConfiguration;
import org.freeplane.plugin.ai.tools.AIToolSet;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummaryHandler;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.memory.ChatMemory;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AIChatServiceFactory {

    private AIChatServiceFactory() {
    }

    public static AIChatService createService(AIToolSet toolSet, ChatMemory chatMemory,
                                              ChatTokenUsageTracker chatTokenUsageTracker,
                                              ToolCallSummaryHandler toolCallSummaryHandler) {
        return createService(toolSet, chatMemory, chatTokenUsageTracker, toolCallSummaryHandler, null, null);
    }

    public static AIChatService createService(AIToolSet toolSet, ChatMemory chatMemory,
                                              ChatTokenUsageTracker chatTokenUsageTracker,
                                              ToolCallSummaryHandler toolCallSummaryHandler,
                                              Supplier<Boolean> cancellationSupplier) {
        return createService(toolSet, chatMemory, chatTokenUsageTracker, toolCallSummaryHandler,
            cancellationSupplier, null);
    }

    public static AIChatService createService(AIToolSet toolSet, ChatMemory chatMemory,
                                              ChatTokenUsageTracker chatTokenUsageTracker,
                                              ToolCallSummaryHandler toolCallSummaryHandler,
                                              Supplier<Boolean> cancellationSupplier,
                                              Consumer<TokenUsage> tokenUsageConsumer) {
        return createService(toolSet, chatMemory, chatTokenUsageTracker, toolCallSummaryHandler,
            cancellationSupplier, tokenUsageConsumer, null, null);
    }

    public static AIChatService createService(AIToolSet toolSet, ChatMemory chatMemory,
                                              ChatTokenUsageTracker chatTokenUsageTracker,
                                              ToolCallSummaryHandler toolCallSummaryHandler,
                                              Supplier<Boolean> cancellationSupplier,
                                              Consumer<TokenUsage> tokenUsageConsumer,
                                              Supplier<ChatToolAvailability> toolAvailabilitySupplier) {
        return createService(toolSet, chatMemory, chatTokenUsageTracker, toolCallSummaryHandler,
            cancellationSupplier, tokenUsageConsumer, toolAvailabilitySupplier, null);
    }

    public static AIChatService createService(AIToolSet toolSet, ChatMemory chatMemory,
                                              ChatTokenUsageTracker chatTokenUsageTracker,
                                              ToolCallSummaryHandler toolCallSummaryHandler,
                                              Supplier<Boolean> cancellationSupplier,
                                              Consumer<TokenUsage> tokenUsageConsumer,
                                              Supplier<ChatToolAvailability> toolAvailabilitySupplier,
                                              String selectedModelOverride) {
        AIProviderConfiguration configuration = new AIProviderConfiguration(selectedModelOverride);
        ChatModel chatLanguageModel = AIChatModelFactory.createChatLanguageModel(configuration);
        if (toolAvailabilitySupplier == null) {
            return new AIChatService(chatLanguageModel, toolSet, chatMemory,
                chatTokenUsageTracker, toolCallSummaryHandler, cancellationSupplier, tokenUsageConsumer);
        }
        return new AIChatService(chatLanguageModel, toolSet, chatMemory,
            chatTokenUsageTracker, toolCallSummaryHandler, cancellationSupplier, tokenUsageConsumer,
            toolAvailabilitySupplier, null);
    }
}
