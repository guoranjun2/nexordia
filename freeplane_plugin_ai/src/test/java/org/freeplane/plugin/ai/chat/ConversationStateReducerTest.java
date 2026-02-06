package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ConversationStateReducerTest {

    @Test
    public void sendUserMessage_startsPendingRequestAndClearsDraft() {
        ConversationStateReducer uut = new ConversationStateReducer();
        ConversationState initialState = ConversationState.empty().withInputDraft("draft");

        ConversationState reducedState = uut.reduce(initialState,
            ConversationCommand.sendUserMessage("hello"));

        assertThat(reducedState.isRequestInProgress()).isTrue();
        assertThat(reducedState.getPendingUserMessage()).isEqualTo("hello");
        assertThat(reducedState.getInputDraft()).isEmpty();
        assertThat(reducedState.getTurns()).isEmpty();
    }

    @Test
    public void appendAssistantResponse_completesPendingTurn() {
        ConversationStateReducer uut = new ConversationStateReducer();
        ConversationState initialState = ConversationState.empty()
            .withPendingUserMessage("hello");

        ConversationState reducedState = uut.reduce(initialState,
            ConversationCommand.appendAssistantResponse("hi there"));

        assertThat(reducedState.isRequestInProgress()).isFalse();
        assertThat(reducedState.getPendingUserMessage()).isNull();
        assertThat(reducedState.getTurns()).hasSize(1);
        assertThat(reducedState.getTurns().get(0).getUserMessage()).isEqualTo("hello");
        assertThat(reducedState.getTurns().get(0).getAssistantMessage()).isEqualTo("hi there");
    }

    @Test
    public void cancelActiveRequest_restoresPendingMessageToDraft() {
        ConversationStateReducer uut = new ConversationStateReducer();
        ConversationState initialState = ConversationState.empty()
            .withPendingUserMessage("restore me");

        ConversationState reducedState = uut.reduce(initialState,
            ConversationCommand.cancelActiveRequest());

        assertThat(reducedState.isRequestInProgress()).isFalse();
        assertThat(reducedState.getPendingUserMessage()).isNull();
        assertThat(reducedState.getInputDraft()).isEqualTo("restore me");
        assertThat(reducedState.getTurns()).isEmpty();
    }

    @Test
    public void appendAssistantResponse_withoutPendingRequest_keepsState() {
        ConversationStateReducer uut = new ConversationStateReducer();
        ConversationState initialState = ConversationState.empty();

        ConversationState reducedState = uut.reduce(initialState,
            ConversationCommand.appendAssistantResponse("ignored"));

        assertThat(reducedState).isEqualTo(initialState);
    }
}
