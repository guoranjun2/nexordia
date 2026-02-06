package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.memory.ChatMemory;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
public class AssistantProfileSelectionSyncTest {

    @Test
    public void applyAssistantProfileSelection_emitsOnlyProfilePaneMessage() {
        AssistantProfileSelectionModel selectionModel = mock(AssistantProfileSelectionModel.class);
        LiveChatController liveChatController = mock(LiveChatController.class);
        ChatSessionMemoryController chatSessionMemoryController = mock(ChatSessionMemoryController.class);
        ChatMemory chatMemory = mock(ChatMemory.class);
        when(chatSessionMemoryController.getChatMemory()).thenReturn(chatMemory);
        AssistantProfileSelectionSync uut = new AssistantProfileSelectionSync(
            selectionModel, liveChatController);
        uut.setChatSessionMemoryController(chatSessionMemoryController);
        List<String> paneMessages = new ArrayList<>();
        uut.setProfileMessageConsumer(paneMessages::add);
        AssistantProfile profile = new AssistantProfile("profile-id", "A sayer", "Start with A");

        uut.applyAssistantProfileSelection(profile);

        verify(chatMemory).add(any(AssistantProfileSystemMessage.class));
        verify(liveChatController).recordAssistantProfileMessage(
            argThat(message -> "A sayer".equals(message.getProfileName())
                && "Start with A".equals(message.getProfileDefinition())
                && !message.isHistoricalMarker()));
        assertThat(paneMessages).containsExactly("A sayer");
    }
}
