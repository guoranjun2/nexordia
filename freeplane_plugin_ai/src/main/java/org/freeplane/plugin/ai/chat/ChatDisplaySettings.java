package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public class ChatDisplaySettings {
    private static final String CHAT_SHOWS_TOOL_CALLS_PROPERTY = "ai_chat_shows_tool_calls";

    private final ResourceController resourceController;

    public ChatDisplaySettings() {
        this.resourceController = ResourceController.getResourceController();
    }

    public boolean isToolCallHistoryVisible() {
        return resourceController.getBooleanProperty(CHAT_SHOWS_TOOL_CALLS_PROPERTY);
    }
}
