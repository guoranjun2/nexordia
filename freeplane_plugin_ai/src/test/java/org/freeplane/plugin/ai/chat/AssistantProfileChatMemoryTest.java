package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.List;
import org.junit.Test;

public class AssistantProfileChatMemoryTest {

    @Test
    public void messages_ordersSystemMessagesBySlot() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(10);

        uut.add(UserMessage.from("hello"));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(new AssistantProfileSystemMessage("profile"));
        uut.add(new GeneralSystemMessage("general"));
        uut.add(new RemovedForSpaceSystemMessage("removed"));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages).hasSize(5);
        assertThat(messages.get(0)).isInstanceOf(GeneralSystemMessage.class);
        assertThat(messages.get(1)).isInstanceOf(AssistantProfileSystemMessage.class);
        assertThat(messages.get(2)).isInstanceOf(TranscriptHiddenSystemMessage.class);
        assertThat(messages.get(3)).isInstanceOf(RemovedForSpaceSystemMessage.class);
        assertThat(messages.get(4)).isInstanceOf(UserMessage.class);
    }

    @Test
    public void capacity_excludesTranscriptHiddenAndRemovedForSpace() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(2);

        uut.add(new GeneralSystemMessage("general"));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(UserMessage.from("first"));
        uut.add(AiMessage.from("second"));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages).hasSize(4);
        assertThat(messages.get(0)).isInstanceOf(GeneralSystemMessage.class);
        assertThat(messages.get(1)).isInstanceOf(TranscriptHiddenSystemMessage.class);
        assertThat(messages.get(2)).isInstanceOf(RemovedForSpaceSystemMessage.class);
        assertThat(messages.get(3)).isInstanceOf(AiMessage.class);
    }

    @Test
    public void assistantProfileMessagesDropWhenNoConversationMessagesRemain() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(1);

        uut.add(new AssistantProfileSystemMessage("profile"));
        uut.add(UserMessage.from("first"));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages)
            .extracting(Object::getClass)
            .containsExactly(RemovedForSpaceSystemMessage.class);
    }

    @Test
    public void removedForSpaceMessageInsertedOnce() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(1);

        uut.add(UserMessage.from("first"));
        uut.add(AiMessage.from("second"));
        uut.add(AiMessage.from("third"));

        List<ChatMessage> messages = uut.messages();
        long removedCount = messages.stream()
            .filter(message -> message instanceof RemovedForSpaceSystemMessage)
            .count();
        assertThat(removedCount).isEqualTo(1);
    }

    @Test
    public void evictingToolRequestAlsoEvictsToolResults() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(1);
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
            .id("tool-1")
            .name("test")
            .arguments("{}")
            .build();

        uut.add(AiMessage.from(List.of(toolRequest)));
        uut.add(ToolExecutionResultMessage.from("tool-1", "test", "result"));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages).noneMatch(message -> message instanceof ToolExecutionResultMessage);
        assertThat(messages).noneMatch(message -> message instanceof AiMessage);
    }
}
