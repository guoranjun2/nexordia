package org.freeplane.plugin.ai.chat;

class ConversationStateReducer {

    ConversationState reduce(ConversationState currentState, ConversationCommand command) {
        if (currentState == null || command == null || command.getType() == null) {
            return currentState;
        }
        if (command.getType() == ConversationCommandType.SEND_USER_MESSAGE) {
            return currentState.withPendingUserMessage(command.getText());
        }
        if (command.getType() == ConversationCommandType.APPEND_ASSISTANT_RESPONSE) {
            return currentState.withAssistantResponse(command.getText());
        }
        if (command.getType() == ConversationCommandType.CANCEL_ACTIVE_REQUEST) {
            return currentState.withoutPendingRequest();
        }
        return currentState;
    }
}
