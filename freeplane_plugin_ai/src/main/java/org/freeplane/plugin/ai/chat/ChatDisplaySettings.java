package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public final class ChatDisplaySettings {
    private static final String CHAT_SHOW_TOOL_CALLS_PROPERTY = "ai_chat_show_tool_calls";

    private final ResourceController resourceController;

    public ChatDisplaySettings() {
        this.resourceController = ResourceController.getResourceController();
    }

    public boolean isToolCallHistoryVisible() {
        return resourceController.getBooleanProperty(CHAT_SHOW_TOOL_CALLS_PROPERTY);
    }
}
