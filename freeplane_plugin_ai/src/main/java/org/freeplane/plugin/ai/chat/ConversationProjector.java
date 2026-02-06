package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.ArrayList;
import java.util.List;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;

class ConversationProjector {

    private static final String USER_STYLE_CLASS = "message-user";
    private static final String ASSISTANT_STYLE_CLASS = "message-assistant";

    List<ChatMessageHistory.ChatMessageSnapshot> toMessageSnapshots(ConversationState state) {
        List<ChatMessageHistory.ChatMessageSnapshot> snapshots = new ArrayList<ChatMessageHistory.ChatMessageSnapshot>();
        if (state == null) {
            return snapshots;
        }
        for (ConversationTurn turn : state.getTurns()) {
            appendUserSnapshot(snapshots, turn.getUserMessage());
            appendAssistantSnapshot(snapshots, turn.getAssistantMessage());
        }
        if (state.getPendingUserMessage() != null) {
            appendUserSnapshot(snapshots, state.getPendingUserMessage());
        }
        return snapshots;
    }

    List<ChatMessage> toChatMemoryMessages(ConversationState state) {
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        if (state == null) {
            return messages;
        }
        for (ConversationTurn turn : state.getTurns()) {
            messages.add(UserMessage.from(turn.getUserMessage()));
            messages.add(AiMessage.from(turn.getAssistantMessage()));
        }
        if (state.getPendingUserMessage() != null) {
            messages.add(UserMessage.from(state.getPendingUserMessage()));
        }
        return messages;
    }

    List<ChatTranscriptEntry> toTranscriptEntries(ConversationState state) {
        List<ChatTranscriptEntry> entries = new ArrayList<ChatTranscriptEntry>();
        if (state == null) {
            return entries;
        }
        for (ConversationTurn turn : state.getTurns()) {
            entries.add(new ChatTranscriptEntry(ChatTranscriptRole.USER, turn.getUserMessage()));
            entries.add(new ChatTranscriptEntry(ChatTranscriptRole.ASSISTANT, turn.getAssistantMessage()));
        }
        if (state.getPendingUserMessage() != null) {
            entries.add(new ChatTranscriptEntry(ChatTranscriptRole.USER, state.getPendingUserMessage()));
        }
        return entries;
    }

    String toInputDraft(ConversationState state) {
        if (state == null) {
            return "";
        }
        return state.getInputDraft();
    }

    private void appendUserSnapshot(List<ChatMessageHistory.ChatMessageSnapshot> snapshots, String text) {
        String safeText = text == null ? "" : text;
        snapshots.add(new ChatMessageHistory.ChatMessageSnapshot(
            safeText,
            "<div class=\"" + USER_STYLE_CLASS + "\">" + escapeHtml(safeText) + "</div>",
            USER_STYLE_CLASS));
    }

    private void appendAssistantSnapshot(List<ChatMessageHistory.ChatMessageSnapshot> snapshots, String text) {
        String safeText = text == null ? "" : text;
        snapshots.add(new ChatMessageHistory.ChatMessageSnapshot(
            safeText,
            "<div class=\"" + ASSISTANT_STYLE_CLASS + "\">" + escapeHtml(safeText) + "</div>",
            ASSISTANT_STYLE_CLASS));
    }

    private String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }
}
