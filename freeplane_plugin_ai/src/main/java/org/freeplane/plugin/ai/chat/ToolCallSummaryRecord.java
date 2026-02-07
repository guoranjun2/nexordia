package org.freeplane.plugin.ai.chat;

import org.freeplane.plugin.ai.tools.utilities.ToolCaller;

class ToolCallSummaryRecord {

    private int conversationIndex;
    private final String summaryText;
    private final ToolCaller toolCaller;

    ToolCallSummaryRecord(int conversationIndex, String summaryText, ToolCaller toolCaller) {
        this.conversationIndex = Math.max(0, conversationIndex);
        this.summaryText = summaryText == null ? "" : summaryText;
        this.toolCaller = toolCaller == null ? ToolCaller.CHAT : toolCaller;
    }

    int conversationIndex() {
        return conversationIndex;
    }

    String summaryText() {
        return summaryText;
    }

    ToolCaller toolCaller() {
        return toolCaller;
    }

    void shiftForInsert(int insertIndex) {
        if (conversationIndex > insertIndex) {
            conversationIndex++;
        }
    }

    void shiftForRemove(int removeIndex) {
        if (conversationIndex > removeIndex) {
            conversationIndex--;
        }
    }
}
