package org.freeplane.plugin.ai.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

class ConversationState {

    private final List<ConversationTurn> turns;
    private final String inputDraft;
    private final String pendingUserMessage;
    private final boolean requestInProgress;

    private ConversationState(List<ConversationTurn> turns, String inputDraft, String pendingUserMessage,
                              boolean requestInProgress) {
        this.turns = turns;
        this.inputDraft = inputDraft;
        this.pendingUserMessage = pendingUserMessage;
        this.requestInProgress = requestInProgress;
    }

    static ConversationState empty() {
        return new ConversationState(new ArrayList<ConversationTurn>(), "", null, false);
    }

    List<ConversationTurn> getTurns() {
        return Collections.unmodifiableList(turns);
    }

    String getInputDraft() {
        return inputDraft;
    }

    String getPendingUserMessage() {
        return pendingUserMessage;
    }

    boolean isRequestInProgress() {
        return requestInProgress;
    }

    ConversationState withInputDraft(String draft) {
        return new ConversationState(new ArrayList<ConversationTurn>(turns),
            draft == null ? "" : draft,
            pendingUserMessage,
            requestInProgress);
    }

    ConversationState withPendingUserMessage(String userMessage) {
        String normalizedUserMessage = userMessage == null ? "" : userMessage.trim();
        if (normalizedUserMessage.isEmpty()) {
            return this;
        }
        return new ConversationState(new ArrayList<ConversationTurn>(turns), "",
            normalizedUserMessage, true);
    }

    ConversationState withAssistantResponse(String assistantMessage) {
        if (pendingUserMessage == null) {
            return this;
        }
        List<ConversationTurn> updatedTurns = new ArrayList<ConversationTurn>(turns);
        updatedTurns.add(new ConversationTurn(pendingUserMessage,
            assistantMessage == null ? "" : assistantMessage));
        return new ConversationState(updatedTurns, "", null, false);
    }

    ConversationState withoutPendingRequest() {
        if (pendingUserMessage == null) {
            return this;
        }
        return new ConversationState(new ArrayList<ConversationTurn>(turns),
            pendingUserMessage, null, false);
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof ConversationState)) {
            return false;
        }
        ConversationState other = (ConversationState) otherObject;
        return requestInProgress == other.requestInProgress
            && Objects.equals(turns, other.turns)
            && Objects.equals(inputDraft, other.inputDraft)
            && Objects.equals(pendingUserMessage, other.pendingUserMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(turns, inputDraft, pendingUserMessage, requestInProgress);
    }
}
