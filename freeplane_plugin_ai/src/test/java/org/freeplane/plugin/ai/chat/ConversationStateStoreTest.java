package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ConversationStateStoreTest {

    @Test
    public void undo_movesBackToPreviousState() {
        ConversationStateStore uut = new ConversationStateStore();
        uut.dispatch(ConversationCommand.sendUserMessage("first"));
        uut.dispatch(ConversationCommand.appendAssistantResponse("one"));
        uut.dispatch(ConversationCommand.sendUserMessage("second"));
        uut.dispatch(ConversationCommand.appendAssistantResponse("two"));

        uut.dispatch(ConversationCommand.undo());

        assertThat(uut.getCurrentState().getTurns()).hasSize(1);
        assertThat(uut.getCurrentState().getTurns().get(0).getUserMessage()).isEqualTo("first");
        assertThat(uut.canRedo()).isTrue();
    }

    @Test
    public void redo_restoresUndoneState() {
        ConversationStateStore uut = new ConversationStateStore();
        uut.dispatch(ConversationCommand.sendUserMessage("first"));
        uut.dispatch(ConversationCommand.appendAssistantResponse("one"));
        uut.dispatch(ConversationCommand.sendUserMessage("second"));
        uut.dispatch(ConversationCommand.appendAssistantResponse("two"));
        uut.dispatch(ConversationCommand.undo());

        uut.dispatch(ConversationCommand.redo());

        assertThat(uut.getCurrentState().getTurns()).hasSize(2);
        assertThat(uut.getCurrentState().getTurns().get(1).getUserMessage()).isEqualTo("second");
        assertThat(uut.canRedo()).isFalse();
    }

    @Test
    public void sendingAfterUndo_clearsRedoHistory() {
        ConversationStateStore uut = new ConversationStateStore();
        uut.dispatch(ConversationCommand.sendUserMessage("first"));
        uut.dispatch(ConversationCommand.appendAssistantResponse("one"));
        uut.dispatch(ConversationCommand.sendUserMessage("second"));
        uut.dispatch(ConversationCommand.appendAssistantResponse("two"));
        uut.dispatch(ConversationCommand.undo());
        assertThat(uut.canRedo()).isTrue();

        uut.dispatch(ConversationCommand.sendUserMessage("replacement"));

        assertThat(uut.canRedo()).isFalse();
    }
}
