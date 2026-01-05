package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public final class SystemMessageBuilder {
	public static final String SYSTEM_MESSAGE_PROPERTY = "ai_system_message";
    @FunctionalInterface
    interface SystemMessageTextProvider {
        String getSystemMessageText();
    }

    private final SystemMessageTextProvider systemMessageTextProvider;

    public SystemMessageBuilder() {
        this(new ResourceControllerSystemMessageTextProvider());
    }

    SystemMessageBuilder(SystemMessageTextProvider systemMessageTextProvider) {
        this.systemMessageTextProvider = systemMessageTextProvider;
    }

    public String buildForChat() {
        String message = systemMessageTextProvider.getSystemMessageText();
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? null : message;
    }

    private static final class ResourceControllerSystemMessageTextProvider implements SystemMessageTextProvider {

        @Override
        public String getSystemMessageText() {
            ResourceController resourceController = ResourceController.getResourceController();
            String message = resourceController.getProperty(SYSTEM_MESSAGE_PROPERTY);
            return message;
        }
    }
}
