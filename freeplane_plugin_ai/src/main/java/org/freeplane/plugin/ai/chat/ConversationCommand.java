package org.freeplane.plugin.ai.chat;

class ConversationCommand {

    private final ConversationCommandType type;
    private final String text;

    private ConversationCommand(ConversationCommandType type, String text) {
        this.type = type;
        this.text = text;
    }

    static ConversationCommand sendUserMessage(String text) {
        return new ConversationCommand(ConversationCommandType.SEND_USER_MESSAGE, text);
    }

    static ConversationCommand appendAssistantResponse(String text) {
        return new ConversationCommand(ConversationCommandType.APPEND_ASSISTANT_RESPONSE, text);
    }

    static ConversationCommand cancelActiveRequest() {
        return new ConversationCommand(ConversationCommandType.CANCEL_ACTIVE_REQUEST, null);
    }

    static ConversationCommand undo() {
        return new ConversationCommand(ConversationCommandType.UNDO, null);
    }

    static ConversationCommand redo() {
        return new ConversationCommand(ConversationCommandType.REDO, null);
    }

    ConversationCommandType getType() {
        return type;
    }

    String getText() {
        return text;
    }
}
