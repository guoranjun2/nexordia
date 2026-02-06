package org.freeplane.plugin.ai.chat;

class ConversationStateStore {

    private final ConversationStateReducer stateReducer;
    private ConversationHistory conversationHistory;

    ConversationStateStore() {
        this(ConversationState.empty(), new ConversationStateReducer());
    }

    ConversationStateStore(ConversationState initialState, ConversationStateReducer stateReducer) {
        this.stateReducer = stateReducer;
        this.conversationHistory = ConversationHistory.withInitialState(initialState);
    }

    void dispatch(ConversationCommand command) {
        if (command == null || command.getType() == null) {
            return;
        }
        if (command.getType() == ConversationCommandType.UNDO) {
            conversationHistory = conversationHistory.undo();
            return;
        }
        if (command.getType() == ConversationCommandType.REDO) {
            conversationHistory = conversationHistory.redo();
            return;
        }
        ConversationState currentState = conversationHistory.getCurrentState();
        ConversationState nextState = stateReducer.reduce(currentState, command);
        boolean clearFuture = command.getType() == ConversationCommandType.SEND_USER_MESSAGE;
        conversationHistory = conversationHistory.advance(nextState, clearFuture);
    }

    ConversationState getCurrentState() {
        return conversationHistory.getCurrentState();
    }

    boolean canUndo() {
        return conversationHistory.canUndo();
    }

    boolean canRedo() {
        return conversationHistory.canRedo();
    }
}
