package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import java.util.List;
import org.junit.Test;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class AssistantProfileChatMemoryTest {

    @Test
    public void messages_ordersSystemMessagesBySlot() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(10);

        uut.add(UserMessage.from("hello"));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(new AssistantProfileSystemMessage("profile", "", true));
        uut.add(new GeneralSystemMessage("general"));
        uut.add(new RemovedForSpaceSystemMessage("removed"));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages).hasSize(8);
        assertThat(messages.get(0)).isInstanceOf(GeneralSystemMessage.class);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(1)).singleText())
            .isEqualTo("hello");
        assertThat(messages.get(2)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(2)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + "hidden");
        assertThat(messages.get(3)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((AiMessage) messages.get(3)).text()).isEqualTo("ok");
        assertThat(messages.get(4)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(4)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + "Now you have the profile profile.");
        assertThat(messages.get(5)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((AiMessage) messages.get(5)).text()).isEqualTo("ok");
        assertThat(messages.get(6)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(6)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + "removed");
        assertThat(messages.get(7)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((AiMessage) messages.get(7)).text()).isEqualTo("ok");
    }

    @Test
    public void capacity_excludesTranscriptHiddenAndRemovedForSpace() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(2);

        uut.add(new GeneralSystemMessage("general"));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(UserMessage.from("first"));
        uut.add(AiMessage.from("second"));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages).hasSize(6);
        assertThat(messages.get(0)).isInstanceOf(GeneralSystemMessage.class);
        assertThat(messages.get(1)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(1)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + "hidden");
        assertThat(messages.get(2)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((AiMessage) messages.get(2)).text()).isEqualTo("ok");
        assertThat(messages.get(3)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(3)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + RemovedForSpaceSystemMessage.DEFAULT_TEXT);
        assertThat(messages.get(4)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((AiMessage) messages.get(4)).text()).isEqualTo("ok");
        assertThat(messages.get(5)).isInstanceOf(AiMessage.class);
    }

    @Test
    public void assistantProfileMessagesDropWhenNoConversationMessagesRemain() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(1);

        uut.add(new AssistantProfileSystemMessage("profile", "", true));
        uut.add(UserMessage.from("first"));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(0)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + RemovedForSpaceSystemMessage.DEFAULT_TEXT);
        assertThat(messages.get(1)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("ok");
        assertThat(messages.get(2)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(2)).singleText()).isEqualTo("first");
    }

    @Test
    public void removedForSpaceMessageInsertedOnce() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(1);

        uut.add(UserMessage.from("first"));
        uut.add(AiMessage.from("second"));
        uut.add(AiMessage.from("third"));

        List<ChatMessage> messages = uut.messages();
        long removedCount = messages.stream()
            .filter(message -> message instanceof UserMessage)
            .map(message -> ((UserMessage) message).singleText())
            .filter(text -> MessageBuilder.CONTROL_INSTRUCTION_PREFIX
                .concat(RemovedForSpaceSystemMessage.DEFAULT_TEXT)
                .equals(text))
            .count();
        assertThat(removedCount).isEqualTo(1);
    }

    @Test
    public void olderProfileInstructionsAreCompactedToMarkers() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(10);
        uut.add(new AssistantProfileSystemMessage("Alpha", "First definition", false));
        uut.add(new AssistantProfileSystemMessage("Beta", "Second definition", false));
        uut.add(UserMessage.from("hello"));

        List<ChatMessage> messages = uut.messages();

        assertThat(((UserMessage) messages.get(0)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX
                + "Now you have the profile Alpha.");
        assertThat(((UserMessage) messages.get(2)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX
                + "Now you have the profile Beta.\nProfile definition: Second definition");
    }

    @Test
    public void profileInstructionCompactionPreservesConversationOrder() {
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.withMaxMessages(20);
        uut.add(new AssistantProfileSystemMessage("Default", "Default definition", false));
        uut.add(UserMessage.from("u1"));
        uut.add(new AssistantProfileSystemMessage("A", "A definition", false));
        uut.add(UserMessage.from("u2"));
        uut.add(new AssistantProfileSystemMessage("Default", "Default definition", false));
        uut.add(UserMessage.from("u3"));

        List<ChatMessage> messages = uut.messages();

        assertThat(((UserMessage) messages.get(0)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX
                + "Now you have the profile Default.");
        assertThat(messages.get(1)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((UserMessage) messages.get(2)).singleText()).isEqualTo("u1");
        assertThat(((UserMessage) messages.get(3)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX
                + "Now you have the profile A.");
        assertThat(messages.get(4)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((UserMessage) messages.get(5)).singleText()).isEqualTo("u2");
        assertThat(((UserMessage) messages.get(6)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX
                + "Now you have the profile Default.\nProfile definition: Default definition");
        assertThat(messages.get(7)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((UserMessage) messages.get(8)).singleText()).isEqualTo("u3");
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
        assertThat(messages).noneMatch(message -> message instanceof AiMessage
            && ((AiMessage) message).hasToolExecutionRequests());
    }
}
