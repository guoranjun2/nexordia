package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public class SystemMessageBuilder {
	public static final String SYSTEM_MESSAGE_PROPERTY = "ai_system_message";
    private static final String TOOL_CALL_REQUEST_WRAPPER_GUIDANCE =
        "Any tool calls in this chat require arguments wrapped under the single parameter named request. "
            + "Example: tool({ \"request\": { ... } })";
    private static final String MARKDOWN_RESPONSE_GUIDANCE = "Respond in Markdown.";
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
            return MARKDOWN_RESPONSE_GUIDANCE + "\n\n" + TOOL_CALL_REQUEST_WRAPPER_GUIDANCE;
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return MARKDOWN_RESPONSE_GUIDANCE + "\n\n" + TOOL_CALL_REQUEST_WRAPPER_GUIDANCE;
        }
        return trimmed + "\n\n" + MARKDOWN_RESPONSE_GUIDANCE + "\n\n" + TOOL_CALL_REQUEST_WRAPPER_GUIDANCE;
    }

    private static class ResourceControllerSystemMessageTextProvider implements SystemMessageTextProvider {

        @Override
        public String getSystemMessageText() {
            ResourceController resourceController = ResourceController.getResourceController();
            String message = resourceController.getProperty(SYSTEM_MESSAGE_PROPERTY);
            return message;
        }
    }
}
