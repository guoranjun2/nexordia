package org.freeplane.plugin.ai.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ConversationHistory {

    private final List<ConversationState> pastStates;
    private final ConversationState currentState;
    private final List<ConversationState> futureStates;

    private ConversationHistory(List<ConversationState> pastStates, ConversationState currentState,
                                List<ConversationState> futureStates) {
        this.pastStates = pastStates;
        this.currentState = currentState;
        this.futureStates = futureStates;
    }

    static ConversationHistory withInitialState(ConversationState initialState) {
        return new ConversationHistory(new ArrayList<ConversationState>(), initialState,
            new ArrayList<ConversationState>());
    }

    List<ConversationState> getPastStates() {
        return Collections.unmodifiableList(pastStates);
    }

    ConversationState getCurrentState() {
        return currentState;
    }

    List<ConversationState> getFutureStates() {
        return Collections.unmodifiableList(futureStates);
    }

    boolean canUndo() {
        return !pastStates.isEmpty();
    }

    boolean canRedo() {
        return !futureStates.isEmpty();
    }

    ConversationHistory undo() {
        if (!canUndo()) {
            return this;
        }
        List<ConversationState> updatedPastStates = new ArrayList<ConversationState>(pastStates);
        ConversationState previousState = updatedPastStates.remove(updatedPastStates.size() - 1);
        List<ConversationState> updatedFutureStates = new ArrayList<ConversationState>();
        updatedFutureStates.add(currentState);
        updatedFutureStates.addAll(futureStates);
        return new ConversationHistory(updatedPastStates, previousState, updatedFutureStates);
    }

    ConversationHistory redo() {
        if (!canRedo()) {
            return this;
        }
        List<ConversationState> updatedFutureStates = new ArrayList<ConversationState>(futureStates);
        ConversationState nextState = updatedFutureStates.remove(0);
        List<ConversationState> updatedPastStates = new ArrayList<ConversationState>(pastStates);
        updatedPastStates.add(currentState);
        return new ConversationHistory(updatedPastStates, nextState, updatedFutureStates);
    }

    ConversationHistory advance(ConversationState nextState, boolean clearFuture) {
        if (nextState == null || nextState.equals(currentState)) {
            return this;
        }
        List<ConversationState> updatedPastStates = new ArrayList<ConversationState>(pastStates);
        updatedPastStates.add(currentState);
        List<ConversationState> updatedFutureStates = clearFuture
            ? new ArrayList<ConversationState>()
            : new ArrayList<ConversationState>(futureStates);
        return new ConversationHistory(updatedPastStates, nextState, updatedFutureStates);
    }
}
