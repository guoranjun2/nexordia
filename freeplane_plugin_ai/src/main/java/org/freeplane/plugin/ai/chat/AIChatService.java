package org.freeplane.plugin.ai.chat;

import static dev.langchain4j.internal.Utils.isNullOrBlank;

import java.util.Objects;
import java.util.function.Consumer;

import org.freeplane.plugin.ai.tools.AIToolSet;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummary;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummaryHandler;
import org.freeplane.plugin.ai.tools.utilities.ToolCaller;
import org.freeplane.plugin.ai.tools.utilities.ToolExecutorFactory;
import org.freeplane.plugin.ai.tools.utilities.ToolExecutorRegistry;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.event.AiServiceResponseReceivedEvent;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;

import org.freeplane.core.util.LogUtils;
import java.util.function.Supplier;

public class AIChatService {
    private static final int MAXIMUM_SUMMARY_TEXT_LENGTH = 160;

    private final AIAssistant assistant;
    private final ToolCallSummaryHandler toolCallSummaryHandler;
    private final ToolArgumentsErrorHandler toolArgumentsErrorHandler;

    public AIChatService(ChatModel chatLanguageModel, AIToolSet toolSet, ChatMemory chatMemory,
                         ChatTokenUsageTracker chatTokenUsageTracker, ToolCallSummaryHandler toolCallSummaryHandler,
                         Supplier<Boolean> cancellationSupplier, Consumer<TokenUsage> tokenUsageConsumer) {
        Objects.requireNonNull(chatTokenUsageTracker, "chatTokenUsageTracker");
        this.toolCallSummaryHandler = toolCallSummaryHandler;
        this.toolArgumentsErrorHandler = buildToolArgumentsErrorHandler();
        ToolExecutorFactory toolExecutorFactory = new ToolExecutorFactory(true, true, cancellationSupplier);
        ToolExecutorRegistry toolExecutorRegistry = toolExecutorFactory.createRegistry(toolSet);
        AiServices<AIAssistant> builder = AiServices.builder(AIAssistant.class)
            .toolArgumentsErrorHandler(toolArgumentsErrorHandler)
            .chatModel(chatLanguageModel)
            .systemMessageProvider(toolSet::systemMessageForChat)
            .tools(toolExecutorRegistry.getExecutorsBySpecification())
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
                    if (tokenUsageConsumer != null) {
                        tokenUsageConsumer.accept(event.response().tokenUsage());
                    }
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
            });
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

    private ToolArgumentsErrorHandler buildToolArgumentsErrorHandler() {
        return (error, context) -> {
            String errorMessage = isNullOrBlank(error.getMessage()) ? error.getClass().getName() : error.getMessage();
            String toolName = context == null ? null : context.toolExecutionRequest().name();
            String arguments = context == null ? null : context.toolExecutionRequest().arguments();
            if (isNullOrBlank(toolName)) {
                toolName = "unknown tool";
            }
            publishToolArgumentsErrorSummary(toolName, arguments, errorMessage);
            return ToolErrorHandlerResult.text("Tool arguments error for " + toolName + ": " + errorMessage);
        };
    }

    private void publishToolArgumentsErrorSummary(String toolName, String arguments, String errorMessage) {
        if (toolCallSummaryHandler == null) {
            return;
        }
        LogUtils.info(buildToolArgumentsErrorLog(toolName, arguments, errorMessage));
        String summaryText = "tool arguments error: tool=" + sanitizeSummaryValue(toolName);
        String safeArguments = sanitizeSummaryValue(arguments);
        if (!safeArguments.isEmpty()) {
            summaryText = summaryText + ", arguments=" + safeArguments;
        }
        String safeErrorMessage = sanitizeSummaryValue(errorMessage);
        if (!safeErrorMessage.isEmpty()) {
            summaryText = summaryText + ", error=" + safeErrorMessage;
        }
        ToolCallSummary summary = new ToolCallSummary("toolArgumentsError", summaryText, true, ToolCaller.CHAT);
        toolCallSummaryHandler.handleToolCallSummary(summary);
    }

    private String buildToolArgumentsErrorLog(String toolName, String arguments, String errorMessage) {
        String safeToolName = toolName == null ? "unknown tool" : toolName;
        String safeArguments = arguments == null ? "" : arguments;
        String safeError = errorMessage == null ? "" : errorMessage;
        return "Tool arguments error: tool=" + safeToolName + ", arguments=" + safeArguments + ", error=" + safeError;
    }

    private String sanitizeSummaryValue(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").trim();
        if (normalized.length() <= MAXIMUM_SUMMARY_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAXIMUM_SUMMARY_TEXT_LENGTH - 3) + "...";
    }
}
