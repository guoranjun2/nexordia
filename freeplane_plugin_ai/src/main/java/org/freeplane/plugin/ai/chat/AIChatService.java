package org.freeplane.plugin.ai.chat;

import static dev.langchain4j.internal.Utils.isNullOrBlank;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

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

    private AIAssistant assistant;
    private final ToolCallSummaryHandler toolCallSummaryHandler;
    private final ToolArgumentsErrorHandler toolArgumentsErrorHandler;
    private final ChatModel chatLanguageModel;
    private final AIToolSet toolSet;
    private final ChatMemory chatMemory;
    private final ChatTokenUsageTracker chatTokenUsageTracker;
    private final Supplier<Boolean> cancellationSupplier;
    private final Consumer<TokenUsage> tokenUsageConsumer;
    private final ToolExecutorRegistry toolExecutorRegistry;
    private final Supplier<ChatToolAvailability> toolAvailabilitySupplier;
    private final Function<ChatToolAvailability, AIAssistant> assistantFactory;
    private ChatToolAvailability lastToolAvailability;

    public AIChatService(ChatModel chatLanguageModel, AIToolSet toolSet, ChatMemory chatMemory,
                         ChatTokenUsageTracker chatTokenUsageTracker, ToolCallSummaryHandler toolCallSummaryHandler,
                         Supplier<Boolean> cancellationSupplier, Consumer<TokenUsage> tokenUsageConsumer) {
        this(chatLanguageModel, toolSet, chatMemory, chatTokenUsageTracker, toolCallSummaryHandler,
            cancellationSupplier, tokenUsageConsumer,
            new Supplier<ChatToolAvailability>() {
                @Override
                public ChatToolAvailability get() {
                    try {
                        return new ChatToolAvailabilitySettings().getToolAvailability();
                    } catch (Exception ignored) {
                        return ChatToolAvailability.EDITING;
                    }
                }
            },
            null);
    }

    AIChatService(ChatModel chatLanguageModel, AIToolSet toolSet, ChatMemory chatMemory,
                  ChatTokenUsageTracker chatTokenUsageTracker, ToolCallSummaryHandler toolCallSummaryHandler,
                  Supplier<Boolean> cancellationSupplier, Consumer<TokenUsage> tokenUsageConsumer,
                  Supplier<ChatToolAvailability> toolAvailabilitySupplier,
                  Function<ChatToolAvailability, AIAssistant> assistantFactory) {
        Objects.requireNonNull(chatTokenUsageTracker, "chatTokenUsageTracker");
        this.chatLanguageModel = chatLanguageModel;
        this.toolSet = toolSet;
        this.chatMemory = chatMemory;
        this.chatTokenUsageTracker = chatTokenUsageTracker;
        this.toolCallSummaryHandler = toolCallSummaryHandler;
        this.toolArgumentsErrorHandler = buildToolArgumentsErrorHandler();
        this.cancellationSupplier = cancellationSupplier;
        this.tokenUsageConsumer = tokenUsageConsumer;
        ToolExecutorFactory toolExecutorFactory = new ToolExecutorFactory(true, true, cancellationSupplier);
        this.toolExecutorRegistry = toolExecutorFactory.createRegistry(toolSet);
        this.toolAvailabilitySupplier = Objects.requireNonNull(toolAvailabilitySupplier, "toolAvailabilitySupplier");
        this.assistantFactory = assistantFactory != null
            ? assistantFactory
            : new Function<ChatToolAvailability, AIAssistant>() {
                @Override
                public AIAssistant apply(ChatToolAvailability toolAvailability) {
                    return buildAssistant(toolAvailability);
                }
            };
        this.lastToolAvailability = currentToolAvailability();
        this.assistant = this.assistantFactory.apply(lastToolAvailability);
    }

    public String chat(String message) {
        ChatToolAvailability toolAvailability = currentToolAvailability();
        if (toolAvailability != lastToolAvailability) {
            assistant = assistantFactory.apply(toolAvailability);
            lastToolAvailability = toolAvailability;
        }
        return assistant.chat(message);
    }

    private AIAssistant buildAssistant(ChatToolAvailability toolAvailability) {
        AiServices<AIAssistant> builder = AiServices.builder(AIAssistant.class)
            .toolArgumentsErrorHandler(toolArgumentsErrorHandler)
            .chatModel(chatLanguageModel)
            .systemMessageProvider(systemMessageProvider(toolAvailability))
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
        if (toolAvailability.includesTools()) {
            builder.tools(toolExecutorRegistry.filtered(toolAvailability.allowedToolNames())
                .getExecutorsBySpecification());
        }
        if (chatMemory != null) {
            builder.chatMemory(chatMemory);
        }
        return builder.build();
    }

    private ChatToolAvailability currentToolAvailability() {
        ChatToolAvailability toolAvailability = toolAvailabilitySupplier.get();
        return toolAvailability == null
            ? ChatToolAvailability.EDITING
            : toolAvailability;
    }

    Function<Object, String> systemMessageProvider(ChatToolAvailability toolAvailability) {
        final ChatToolAvailability normalizedAvailability = toolAvailability == null
            ? ChatToolAvailability.EDITING
            : toolAvailability;
        return new Function<Object, String>() {
            @Override
            public String apply(Object input) {
                return toolSet.systemMessageForChat(input, normalizedAvailability);
            }
        };
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
