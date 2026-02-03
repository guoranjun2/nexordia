package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;

public class ChatSessionMemoryController {
    private final ChatMemorySettings chatMemorySettings;
    private ChatMemory chatMemory;

    public ChatSessionMemoryController() {
        this(new ChatMemorySettings());
    }

    ChatSessionMemoryController(ChatMemorySettings chatMemorySettings) {
        this.chatMemorySettings = Objects.requireNonNull(chatMemorySettings, "chatMemorySettings");
    }

    public ChatMemory getChatMemory() {
        if (chatMemorySettings.getChatMemoryMode() == ChatMemoryMode.DISABLED) {
            return null;
        }
        if (chatMemory == null) {
            chatMemory = MessageWindowChatMemory.withMaxMessages(chatMemorySettings.getMaximumMessageCount());
        }
        return chatMemory;
    }

    public void clearChatMemory() {
        if (chatMemory != null) {
            chatMemory.clear();
        }
    }

    public List<ChatMessage> snapshotMessages() {
        ChatMemory memory = getChatMemory();
        if (memory == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(memory.messages());
    }

    public void restoreMessages(List<ChatMessage> messages) {
        ChatMemory memory = getChatMemory();
        if (memory == null) {
            return;
        }
        memory.clear();
        if (messages == null) {
            return;
        }
        for (ChatMessage message : messages) {
            if (message != null) {
                memory.add(message);
            }
        }
    }

    public void seedTranscript(Iterable<ChatTranscriptEntry> entries, String systemGuard) {
        ChatMemory memory = getChatMemory();
        if (memory == null) {
            return;
        }
        memory.clear();
        if (systemGuard != null && !systemGuard.trim().isEmpty()) {
            memory.add(new SystemMessage(systemGuard));
        }
        if (entries == null) {
            return;
        }
        for (ChatTranscriptEntry entry : entries) {
            ChatMessage message = toChatMessage(entry);
            if (message != null) {
                memory.add(message);
            }
        }
    }

    public void seedTranscriptWithHiddenExchange(Iterable<ChatTranscriptEntry> entries,
                                                 String hiddenSystemMessage) {
        ChatMemory memory = getChatMemory();
        if (memory == null) {
            return;
        }
        memory.clear();
        if (entries == null) {
            return;
        }
        for (ChatTranscriptEntry entry : entries) {
            ChatMessage message = toChatMessage(entry);
            if (message != null) {
                memory.add(message);
            }
        }
        if (hiddenSystemMessage != null && !hiddenSystemMessage.trim().isEmpty()) {
            memory.add(new SystemMessage(hiddenSystemMessage));
        }
    }

    private ChatMessage toChatMessage(ChatTranscriptEntry entry) {
        if (entry == null || entry.getText() == null || entry.getRole() == null) {
            return null;
        }
        if (entry.getRole() == ChatTranscriptRole.ASSISTANT) {
            return new AiMessage(entry.getText());
        }
        return new UserMessage(entry.getText());
    }
}
