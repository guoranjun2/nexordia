package org.freeplane.plugin.ai.chat;

public final class ChatUsageTotals {
    private final Long inputTokenCount;
    private final Long outputTokenCount;

    ChatUsageTotals(Long inputTokenCount, Long outputTokenCount) {
        this.inputTokenCount = inputTokenCount;
        this.outputTokenCount = outputTokenCount;
    }

    public Long getInputTokenCount() {
        return inputTokenCount;
    }

    public Long getOutputTokenCount() {
        return outputTokenCount;
    }

    public String formatStatusLine() {
        String inputText = inputTokenCount == null ? "not available" : String.valueOf(inputTokenCount);
        String outputText = outputTokenCount == null ? "not available" : String.valueOf(outputTokenCount);
        return "Tokens: input " + inputText + ", output " + outputText;
    }
}
