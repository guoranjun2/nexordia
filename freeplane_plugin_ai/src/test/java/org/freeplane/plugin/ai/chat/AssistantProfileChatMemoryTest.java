package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.TokenCountEstimator;
import java.util.List;
import org.junit.Test;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class AssistantProfileChatMemoryTest {

    @Test
    public void messages_ordersSystemMessagesBySlot() {
        AssistantProfileChatMemory uut = createMemory(500);

        uut.add(UserMessage.from("hello"));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(new AssistantProfileControlInstructionMessage("profile", "profile", "", true));
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
        AssistantProfileChatMemory uut = createMemory(25);

        uut.add(new GeneralSystemMessage("general"));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(UserMessage.from("first first first first first first first first first first "
            + "first first first first first first first first first first first first first first first first"));
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
        AssistantProfileChatMemory uut = createMemory(25);

        uut.add(new AssistantProfileControlInstructionMessage("profile", "profile",
            "word word word word word word word word word word "
                + "word word word word word word word word word word "
                + "word word word word word word word word word word ",
            true));
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
        AssistantProfileChatMemory uut = createMemory(25);

        uut.add(UserMessage.from("first first first first first first first first first first "
            + "first first first first first first first first first first first first first first first first"));
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
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(new AssistantProfileControlInstructionMessage("alpha", "Alpha", "First definition", true));
        uut.add(new AssistantProfileControlInstructionMessage("beta", "Beta", "Second definition", true));
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
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(new AssistantProfileControlInstructionMessage("default", "Default", "Default definition", true));
        uut.add(UserMessage.from("u1"));
        uut.add(new AssistantProfileControlInstructionMessage("a", "A", "A definition", true));
        uut.add(UserMessage.from("u2"));
        uut.add(new AssistantProfileControlInstructionMessage("default", "Default", "Default definition", true));
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
        AssistantProfileChatMemory uut = createMemory(1);
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

    @Test
    public void undoAndRedoTrackLastCompletedTurns() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));

        assertThat(uut.canUndo()).isTrue();
        assertThat(uut.canRedo()).isFalse();
        assertThat(uut.undo()).isEqualTo("u2");
        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u1")
            .doesNotContain("u2");
        assertThat(uut.canRedo()).isTrue();

        uut.redo();

        assertThat(uut.canRedo()).isFalse();
        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u1", "u2");
    }

    @Test
    public void newMessageAfterUndoClearsRedoBranch() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));

        assertThat(uut.undo()).isEqualTo("u2");
        uut.add(UserMessage.from("u3"));
        uut.add(AiMessage.from("a3"));

        assertThat(uut.canRedo()).isFalse();
        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u1", "u3")
            .doesNotContain("u2");
    }

    @Test
    public void tokenCountingUsesDeltaUpdates() {
        CountingWordTokenCountEstimator estimator = new CountingWordTokenCountEstimator();
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.builder()
            .maxTokens(500)
            .tokenCountEstimator(estimator)
            .build();

        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));
        int callsAfterAdds = estimator.getMessageEstimateCalls();
        uut.messages();
        int callsAfterRead = estimator.getMessageEstimateCalls();

        assertThat(callsAfterAdds).isEqualTo(4);
        assertThat(callsAfterRead).isEqualTo(callsAfterAdds);
    }

    @Test
    public void deferredCapacityChecksEvictOnCompletion() {
        AssistantProfileChatMemory uut = createMemory(2);
        uut.deferCapacityChecks();

        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u1", "u2");

        uut.completeDeferredCapacityChecks();

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u2")
            .doesNotContain("u1");
    }

    @Test
    public void evictOldestTurnRemovesFirstTurn() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));

        boolean evicted = uut.evictOldestTurn();

        assertThat(evicted).isTrue();
        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u2")
            .doesNotContain("u1");
    }

    @Test
    public void truncateConversationMessagesPreservesAssistantProfileMessageType() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(new AssistantProfileControlInstructionMessage("profile", "Profile", "definition", true));
        int sizeAfterProfileInjection = uut.conversationMessageCount();
        uut.add(UserMessage.from("u1"));

        uut.truncateConversationMessagesTo(sizeAfterProfileInjection);

        assertThat(uut.activeTranscriptEntries())
            .anyMatch(entry -> entry instanceof AssistantProfileTranscriptEntry);
    }

    @Test
    public void tokenCountingIncludesToolMessagesWithoutFullRecount() {
        CountingWordTokenCountEstimator estimator = new CountingWordTokenCountEstimator();
        AssistantProfileChatMemory uut = AssistantProfileChatMemory.builder()
            .maxTokens(500)
            .tokenCountEstimator(estimator)
            .build();
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
            .id("tool-1")
            .name("test")
            .arguments("{}")
            .build();

        uut.add(AiMessage.from(List.of(toolRequest)));
        uut.add(ToolExecutionResultMessage.from("tool-1", "test", "result"));
        int callsAfterAdds = estimator.getMessageEstimateCalls();
        uut.messages();

        assertThat(callsAfterAdds).isEqualTo(2);
        assertThat(estimator.getMessageEstimateCalls()).isEqualTo(callsAfterAdds);
    }

    @Test
    public void truncateConversationMessagesAdjustsTokenTotalByDelta() {
        AssistantProfileChatMemory uut = createMemory(3);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));

        uut.truncateConversationMessagesTo(2);
        uut.add(AiMessage.from("a2"));

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u1")
            .doesNotContain("u2");
    }

    private AssistantProfileChatMemory createMemory(int maxTokens) {
        return AssistantProfileChatMemory.builder()
            .maxTokens(maxTokens)
            .tokenCountEstimator(new WordCountTokenCountEstimator())
            .build();
    }

    private static class WordCountTokenCountEstimator implements TokenCountEstimator {

        @Override
        public int estimateTokenCountInText(String text) {
            if (text == null) {
                return 1;
            }
            String normalized = text.trim();
            if (normalized.isEmpty()) {
                return 1;
            }
            return normalized.split("\\s+").length;
        }

        @Override
        public int estimateTokenCountInMessage(ChatMessage message) {
            if (message instanceof UserMessage) {
                return estimateTokenCountInText(((UserMessage) message).singleText());
            }
            if (message instanceof AiMessage) {
                return estimateTokenCountInText(((AiMessage) message).text());
            }
            if (message instanceof SystemMessage) {
                return estimateTokenCountInText(((SystemMessage) message).text());
            }
            return estimateTokenCountInText(message.toString());
        }

        @Override
        public int estimateTokenCountInMessages(Iterable<ChatMessage> messages) {
            int total = 0;
            for (ChatMessage message : messages) {
                total += estimateTokenCountInMessage(message);
            }
            return total;
        }
    }

    private static class CountingWordTokenCountEstimator extends WordCountTokenCountEstimator {

        private int messageEstimateCalls;

        @Override
        public int estimateTokenCountInMessage(ChatMessage message) {
            messageEstimateCalls++;
            return super.estimateTokenCountInMessage(message);
        }

        int getMessageEstimateCalls() {
            return messageEstimateCalls;
        }
    }
}
