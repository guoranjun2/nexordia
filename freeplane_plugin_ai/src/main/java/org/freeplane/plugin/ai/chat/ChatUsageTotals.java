package org.freeplane.plugin.ai.chat;

public final class ChatUsageTotals {
    private final long inputTokenCount;
    private final long outputTokenCount;

    ChatUsageTotals(long inputTokenCount, long outputTokenCount) {
        this.inputTokenCount = inputTokenCount;
        this.outputTokenCount = outputTokenCount;
    }

    public long getInputTokenCount() {
        return inputTokenCount;
    }

    public long getOutputTokenCount() {
        return outputTokenCount;
    }

    public String formatStatusLine() {
        return "Tokens: input " + inputTokenCount + ", output " + outputTokenCount;
    }
}
