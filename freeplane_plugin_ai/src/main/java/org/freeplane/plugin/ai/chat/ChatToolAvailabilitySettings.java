package org.freeplane.plugin.ai.chat;

import java.util.Objects;

import org.freeplane.core.resources.ResourceController;

public class ChatToolAvailabilitySettings {
    public static final String CHAT_TOOL_AVAILABILITY_PROPERTY = "ai_chat_tool_availability";

    private final ResourceController resourceController;

    public ChatToolAvailabilitySettings() {
        this(ResourceController.getResourceController());
    }

    ChatToolAvailabilitySettings(ResourceController resourceController) {
        this.resourceController = Objects.requireNonNull(resourceController, "resourceController");
    }

    public ChatToolAvailability getToolAvailability() {
        return ChatToolAvailability.fromPreferenceValue(
            resourceController.getProperty(CHAT_TOOL_AVAILABILITY_PROPERTY));
    }
}
