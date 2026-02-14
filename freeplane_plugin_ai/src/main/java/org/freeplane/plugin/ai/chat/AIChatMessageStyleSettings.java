package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public class AIChatMessageStyleSettings {
    static final String CHAT_FONT_SIZE_PROPERTY = "ai_chat_font_size";
    private static final int DEFAULT_CHAT_FONT_SIZE = 12;

    private final int chatFontSize;

    public AIChatMessageStyleSettings() {
        this(ResourceController.getResourceController());
    }

    AIChatMessageStyleSettings(ResourceController resourceController) {
        this.chatFontSize = parseChatFontSize(resourceController.getProperty(CHAT_FONT_SIZE_PROPERTY));
    }

    public int getChatFontSize() {
        return chatFontSize;
    }

    static int parseChatFontSize(String value) {
        if (value == null || value.isEmpty()) {
            return DEFAULT_CHAT_FONT_SIZE;
        }
        try {
            int parsedValue = Integer.parseInt(value.trim());
            return parsedValue > 0 ? parsedValue : DEFAULT_CHAT_FONT_SIZE;
        }
        catch (NumberFormatException exception) {
            return DEFAULT_CHAT_FONT_SIZE;
        }
    }
}
