package org.freeplane.plugin.ai.chat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.observability.api.event.ToolExecutedEvent;
import org.freeplane.core.util.LogUtils;

import java.util.Objects;
import java.util.function.Consumer;

public final class ChatTokenUsageTracker {
    private final Consumer<ChatUsageTotals> totalsConsumer;
    private Long inputTokenCount;
    private Long outputTokenCount;

    public ChatTokenUsageTracker(Consumer<ChatUsageTotals> totalsConsumer) {
        this.totalsConsumer = Objects.requireNonNull(totalsConsumer, "totalsConsumer");
        publishTotals();
    }

    public synchronized void recordTokenUsage(TokenUsage tokenUsage) {
        if (tokenUsage == null) {
            return;
        }
        Integer inputCount = tokenUsage.inputTokenCount();
        Integer outputCount = tokenUsage.outputTokenCount();
        if (inputCount == null && outputCount == null) {
            return;
        }
        if (inputCount != null) {
            inputTokenCount = sum(inputTokenCount, inputCount.longValue());
        }
        if (outputCount != null) {
            outputTokenCount = sum(outputTokenCount, outputCount.longValue());
        }
        publishTotals();
    }

    public void logToolExecuted(ToolExecutedEvent event) {
        ToolExecutionRequest request = event.request();
        LogUtils.info(buildToolCallLogMessage(request));
    }

    public synchronized void resetTotals() {
        inputTokenCount = null;
        outputTokenCount = null;
        publishTotals();
    }

    private void publishTotals() {
        totalsConsumer.accept(new ChatUsageTotals(inputTokenCount, outputTokenCount));
    }

    private static long sum(Long current, long addition) {
        return (current == null ? 0L : current) + addition;
    }

    private static String buildToolCallLogMessage(ToolExecutionRequest request) {
        String arguments = request.arguments();
        if (arguments == null || arguments.isEmpty()) {
            return "Tool call: " + request.name();
        }
        return "Tool call: " + request.name() + " " + arguments;
    }
}
