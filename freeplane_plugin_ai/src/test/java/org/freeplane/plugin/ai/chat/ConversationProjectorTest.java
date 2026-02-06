package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.List;
import org.junit.Test;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;

public class ConversationProjectorTest {

    @Test
    public void toMessageSnapshots_projectsTurnsWithExpectedStyles() {
        ConversationProjector uut = new ConversationProjector();
        ConversationState state = ConversationState.empty()
            .withPendingUserMessage("first")
            .withAssistantResponse("one")
            .withPendingUserMessage("second")
            .withAssistantResponse("two");

        List<ChatMessageHistory.ChatMessageSnapshot> snapshots = uut.toMessageSnapshots(state);

        assertThat(snapshots).hasSize(4);
        assertThat(snapshots.get(0).getSourceText()).isEqualTo("first");
        assertThat(snapshots.get(0).getStyleClassName()).isEqualTo("message-user");
        assertThat(snapshots.get(1).getSourceText()).isEqualTo("one");
        assertThat(snapshots.get(1).getStyleClassName()).isEqualTo("message-assistant");
    }

    @Test
    public void toMessageSnapshots_includesPendingUserMessage() {
        ConversationProjector uut = new ConversationProjector();
        ConversationState state = ConversationState.empty()
            .withPendingUserMessage("in-flight");

        List<ChatMessageHistory.ChatMessageSnapshot> snapshots = uut.toMessageSnapshots(state);

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getSourceText()).isEqualTo("in-flight");
        assertThat(snapshots.get(0).getStyleClassName()).isEqualTo("message-user");
    }

    @Test
    public void toChatMemoryMessages_projectsTurnsAndPendingUser() {
        ConversationProjector uut = new ConversationProjector();
        ConversationState state = ConversationState.empty()
            .withPendingUserMessage("first")
            .withAssistantResponse("one")
            .withPendingUserMessage("pending");

        List<ChatMessage> messages = uut.toChatMemoryMessages(state);

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(0)).singleText()).isEqualTo("first");
        assertThat(messages.get(1)).isInstanceOf(AiMessage.class);
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("one");
        assertThat(messages.get(2)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(2)).singleText()).isEqualTo("pending");
    }

    @Test
    public void toTranscriptEntries_projectsTurnsAndPendingUser() {
        ConversationProjector uut = new ConversationProjector();
        ConversationState state = ConversationState.empty()
            .withPendingUserMessage("first")
            .withAssistantResponse("one")
            .withPendingUserMessage("pending");

        List<ChatTranscriptEntry> entries = uut.toTranscriptEntries(state);

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0).getRole()).isEqualTo(ChatTranscriptRole.USER);
        assertThat(entries.get(0).getText()).isEqualTo("first");
        assertThat(entries.get(1).getRole()).isEqualTo(ChatTranscriptRole.ASSISTANT);
        assertThat(entries.get(1).getText()).isEqualTo("one");
        assertThat(entries.get(2).getRole()).isEqualTo(ChatTranscriptRole.USER);
        assertThat(entries.get(2).getText()).isEqualTo("pending");
    }

    @Test
    public void toInputDraft_returnsCurrentDraftText() {
        ConversationProjector uut = new ConversationProjector();
        ConversationState state = ConversationState.empty().withInputDraft("draft text");

        String draft = uut.toInputDraft(state);

        assertThat(draft).isEqualTo("draft text");
    }
}
