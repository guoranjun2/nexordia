package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public class ChatMemorySettings {
    private static final String CHAT_MEMORY_MAXIMUM_TOKEN_COUNT_PROPERTY = "ai_chat_memory_maximum_token_count";
    private static final int DEFAULT_MAXIMUM_TOKEN_COUNT = 65536;

    private final int maximumTokenCount;

    public ChatMemorySettings() {
        this(ResourceController.getResourceController());
    }

    ChatMemorySettings(ResourceController resourceController) {
        this.maximumTokenCount = parseMaximumTokenCount(
            resourceController.getProperty(CHAT_MEMORY_MAXIMUM_TOKEN_COUNT_PROPERTY));
    }

    ChatMemorySettings(int maximumTokenCount) {
        this.maximumTokenCount = maximumTokenCount;
    }

    public int getMaximumTokenCount() {
        return maximumTokenCount;
    }

    static int parseMaximumTokenCount(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_MAXIMUM_TOKEN_COUNT;
        }
        try {
            int parsedValue = Integer.parseInt(value.trim());
            return parsedValue > 0 ? parsedValue : DEFAULT_MAXIMUM_TOKEN_COUNT;
        } catch (NumberFormatException exception) {
            return DEFAULT_MAXIMUM_TOKEN_COUNT;
        }
    }
}
