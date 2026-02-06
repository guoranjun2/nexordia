package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.Test;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;

public class LiveTranscriptAdapterTest {

    @Test
    public void appendAssistantProfileMessage_recordsSystemRole() {
        LiveChatSession session = new LiveChatSession(
            LiveChatSessionId.create(),
            new ChatSessionMemoryController(new ChatMemorySettings(ChatMemoryMode.DISABLED, 1)),
            "test");
        LiveTranscriptAdapter adapter = new LiveTranscriptAdapter();
        AssistantProfileSystemMessage message =
            new AssistantProfileSystemMessage("A sayer", "Start with A", false);

        adapter.appendAssistantProfileMessage(session, message);

        List<ChatTranscriptEntry> entries = session.getTranscriptEntries();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getRole()).isEqualTo(ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM);
        assertThat(entries.get(0)).isInstanceOf(AssistantProfileTranscriptEntry.class);
        AssistantProfileTranscriptEntry entry = (AssistantProfileTranscriptEntry) entries.get(0);
        assertThat(entry.getProfileName()).isEqualTo("A sayer");
        assertThat(entry.getProfileDefinition()).isEqualTo("Start with A");
        assertThat(entry.isHistoricalMarker()).isFalse();
    }
}
