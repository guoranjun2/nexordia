package org.freeplane.plugin.ai.chat;

import java.util.Objects;

class ConversationTurn {

    private final String userMessage;
    private final String assistantMessage;

    ConversationTurn(String userMessage, String assistantMessage) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
    }

    String getUserMessage() {
        return userMessage;
    }

    String getAssistantMessage() {
        return assistantMessage;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (this == otherObject) {
            return true;
        }
        if (!(otherObject instanceof ConversationTurn)) {
            return false;
        }
        ConversationTurn other = (ConversationTurn) otherObject;
        return Objects.equals(userMessage, other.userMessage)
            && Objects.equals(assistantMessage, other.assistantMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userMessage, assistantMessage);
    }
}
