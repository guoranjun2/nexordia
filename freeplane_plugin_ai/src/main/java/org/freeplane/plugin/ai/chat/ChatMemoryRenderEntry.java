package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.ChatMessage;
import org.freeplane.plugin.ai.tools.utilities.ToolCaller;

class ChatMemoryRenderEntry {

    private final ChatMessage chatMessage;
    private final String toolSummaryText;
    private final ToolCaller toolCaller;

    private ChatMemoryRenderEntry(ChatMessage chatMessage, String toolSummaryText, ToolCaller toolCaller) {
        this.chatMessage = chatMessage;
        this.toolSummaryText = toolSummaryText;
        this.toolCaller = toolCaller;
    }

    static ChatMemoryRenderEntry forMessage(ChatMessage message) {
        return new ChatMemoryRenderEntry(message, null, null);
    }

    static ChatMemoryRenderEntry forToolSummary(String summaryText, ToolCaller toolCaller) {
        return new ChatMemoryRenderEntry(null, summaryText, toolCaller);
    }

    boolean isToolSummary() {
        return toolSummaryText != null;
    }

    ChatMessage chatMessage() {
        return chatMessage;
    }

    String toolSummaryText() {
        return toolSummaryText;
    }

    ToolCaller toolCaller() {
        return toolCaller;
    }
}
