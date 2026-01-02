package org.freeplane.plugin.ai.chat;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.util.Objects;

public final class ChatSessionMemoryController {
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
}
