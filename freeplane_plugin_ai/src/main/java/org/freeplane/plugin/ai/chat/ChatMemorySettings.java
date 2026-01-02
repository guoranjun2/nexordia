package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public final class ChatMemorySettings {
    private static final String CHAT_MEMORY_MODE_PROPERTY = "ai_chat_memory_mode";
    private static final String CHAT_MEMORY_MAXIMUM_MESSAGE_COUNT_PROPERTY = "ai_chat_memory_maximum_message_count";
    private static final int DEFAULT_MAXIMUM_MESSAGE_COUNT = 40;

    private final ChatMemoryMode chatMemoryMode;
    private final int maximumMessageCount;

    public ChatMemorySettings() {
        this(ResourceController.getResourceController());
    }

    ChatMemorySettings(ResourceController resourceController) {
        this.chatMemoryMode = ChatMemoryMode.fromPropertyValue(
            resourceController.getProperty(CHAT_MEMORY_MODE_PROPERTY));
        this.maximumMessageCount = parseMaximumMessageCount(
            resourceController.getProperty(CHAT_MEMORY_MAXIMUM_MESSAGE_COUNT_PROPERTY));
    }

    ChatMemorySettings(ChatMemoryMode chatMemoryMode, int maximumMessageCount) {
        this.chatMemoryMode = chatMemoryMode;
        this.maximumMessageCount = maximumMessageCount;
    }

    public ChatMemoryMode getChatMemoryMode() {
        return chatMemoryMode;
    }

    public int getMaximumMessageCount() {
        return maximumMessageCount;
    }

    private static int parseMaximumMessageCount(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_MAXIMUM_MESSAGE_COUNT;
        }
        try {
            int parsedValue = Integer.parseInt(value.trim());
            return parsedValue > 0 ? parsedValue : DEFAULT_MAXIMUM_MESSAGE_COUNT;
        } catch (NumberFormatException exception) {
            return DEFAULT_MAXIMUM_MESSAGE_COUNT;
        }
    }
}
