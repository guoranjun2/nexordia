package org.freeplane.plugin.ai.chat;

import static dev.langchain4j.internal.Utils.isNullOrBlank;

import java.util.Objects;

import org.freeplane.plugin.ai.tools.AIToolSet;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;

public class AIChatService {
    private static final ToolArgumentsErrorHandler TOOL_ARGUMENTS_ERROR_HANDLER = (error, context) -> {
        String errorMessage = isNullOrBlank(error.getMessage()) ? error.getClass().getName() : error.getMessage();
        String toolName = context == null ? null : context.toolExecutionRequest().name();
        if (isNullOrBlank(toolName)) {
            toolName = "unknown tool";
        }
        return ToolErrorHandlerResult.text("Tool arguments error for " + toolName + ": " + errorMessage);
    };

    private final AIAssistant assistant;

    public AIChatService(ChatModel chatLanguageModel, AIToolSet toolSet, ChatMemory chatMemory,
                         ChatTokenUsageTracker chatTokenUsageTracker) {
        Objects.requireNonNull(chatTokenUsageTracker, "chatTokenUsageTracker");
        AiServices<AIAssistant> builder = AiServices.builder(AIAssistant.class)
        	.toolArgumentsErrorHandler(TOOL_ARGUMENTS_ERROR_HANDLER)
            .chatModel(chatLanguageModel)
            .systemMessageProvider(toolSet::systemMessageForChat)
            .registerListener(new AiServiceListener<AiServiceErrorEvent>() {

                @Override
                public Class<AiServiceErrorEvent> getEventClass() {
                    return AiServiceErrorEvent.class;
                }

                @Override
                public void onEvent(AiServiceErrorEvent event) {
                    event.error().printStackTrace();
                }

            })
            .registerListener(new AiServiceListener<AiServiceResponseReceivedEvent>() {

                @Override
                public Class<AiServiceResponseReceivedEvent> getEventClass() {
                    return AiServiceResponseReceivedEvent.class;
                }

                @Override
                public void onEvent(AiServiceResponseReceivedEvent event) {
                    chatTokenUsageTracker.recordTokenUsage(event.response().tokenUsage());
                }

            })
            .registerListener(new AiServiceListener<ToolExecutedEvent>() {

                @Override
                public Class<ToolExecutedEvent> getEventClass() {
                    return ToolExecutedEvent.class;
                }

                @Override
                public void onEvent(ToolExecutedEvent event) {
                    chatTokenUsageTracker.logToolExecuted(event);
                }
            })
            .tools(toolSet);
        if (chatMemory != null) {
            builder.chatMemory(chatMemory);
        }
        this.assistant = builder.build();
    }

    public String chat(String message) {
        return assistant.chat(message);
    }

    public interface AIAssistant {
        String chat(String message);
    }
}
