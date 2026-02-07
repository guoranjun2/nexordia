package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.TokenCountEstimator;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class TranscriptMemoryMapperTest {
    @Test
    public void seedTranscriptWithHiddenExchange_appendsHiddenMessagesAfterTranscript() {
        TranscriptMemoryMapper uut = new TranscriptMemoryMapper();
        AssistantProfileChatMemory memory = AssistantProfileChatMemory.withMaxTokens(500);
        List<ChatTranscriptEntry> entries = Arrays.asList(
            new ChatTranscriptEntry(ChatTranscriptRole.USER, "first user"),
            new ChatTranscriptEntry(ChatTranscriptRole.ASSISTANT, "first assistant"));

        uut.seedTranscriptWithHiddenExchange(memory, entries, "hidden user");

        List<ChatMessage> messages = memory.messages();
        assertThat(messages).hasSize(4);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(0)).singleText())
            .isEqualTo("first user");
        assertThat(messages.get(1)).isInstanceOf(AiMessage.class);
        assertThat(((AiMessage) messages.get(1)).text()).isEqualTo("first assistant");
        assertThat(messages.get(2)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(2)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + "hidden user");
        assertThat(messages.get(3)).isInstanceOf(InstructionAckMessage.class);
        assertThat(((AiMessage) messages.get(3)).text()).isEqualTo("ok");
    }

    @Test
    public void seedTranscriptWithHiddenExchange_mapsAssistantProfileSubtypeWithoutText() {
        TranscriptMemoryMapper uut = new TranscriptMemoryMapper();
        AssistantProfileChatMemory memory = AssistantProfileChatMemory.withMaxTokens(500);
        List<ChatTranscriptEntry> entries = Arrays.asList(
            new AssistantProfileTranscriptEntry("profile-a", "A sayer", true),
            new ChatTranscriptEntry(ChatTranscriptRole.USER, "hello"));

        uut.seedTranscriptWithHiddenExchange(memory, entries, "hidden user");

        List<ChatMessage> messages = memory.messages();
        assertThat(messages).hasSize(5);
        assertThat(messages.get(0)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(0)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + "Now you have the profile A sayer.");
        assertThat(messages.get(1)).isInstanceOf(InstructionAckMessage.class);
        assertThat(messages.get(2)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(2)).singleText()).isEqualTo("hello");
        assertThat(messages.get(3)).isInstanceOf(UserMessage.class);
        assertThat(((UserMessage) messages.get(3)).singleText())
            .isEqualTo(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + "hidden user");
        assertThat(messages.get(4)).isInstanceOf(InstructionAckMessage.class);
    }

    @Test
    public void toTranscriptEntries_usesVisibleConversationMessagesOnly() {
        TranscriptMemoryMapper uut = new TranscriptMemoryMapper();
        AssistantProfileChatMemory memory = AssistantProfileChatMemory.withMaxTokens(500);
        memory.add(new AssistantProfileControlInstructionMessage("profile-a", "A sayer", "Start with A", true));
        memory.add(UserMessage.from("hello"));
        memory.add(AiMessage.from("world"));
        memory.add(new TranscriptHiddenSystemMessage("hidden"));

        List<ChatTranscriptEntry> entries = uut.toTranscriptEntries(memory);

        assertThat(entries).hasSize(3);
        assertThat(entries.get(0)).isInstanceOf(AssistantProfileTranscriptEntry.class);
        assertThat(entries.get(0).getRole()).isEqualTo(ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM);
        assertThat(entries.get(1).getRole()).isEqualTo(ChatTranscriptRole.USER);
        assertThat(entries.get(1).getText()).isEqualTo("hello");
        assertThat(entries.get(2).getRole()).isEqualTo(ChatTranscriptRole.ASSISTANT);
        assertThat(entries.get(2).getText()).isEqualTo("world");
    }

    @Test
    public void transcriptRestoredMessagesUseLocalTokenAccounting() {
        TranscriptMemoryMapper uut = new TranscriptMemoryMapper();
        CountingWordTokenCountEstimator estimator = new CountingWordTokenCountEstimator();
        AssistantProfileChatMemory memory = AssistantProfileChatMemory.builder()
            .maxTokens(500)
            .tokenCountEstimator(estimator)
            .build();
        List<ChatTranscriptEntry> entries = Arrays.asList(
            new ChatTranscriptEntry(ChatTranscriptRole.USER, "one"),
            new ChatTranscriptEntry(ChatTranscriptRole.ASSISTANT, "two"),
            new ChatTranscriptEntry(ChatTranscriptRole.USER, "three"));

        uut.seedTranscriptWithHiddenExchange(memory, entries, null);
        int callsAfterSeed = estimator.getMessageEstimateCalls();
        memory.messages();

        assertThat(callsAfterSeed).isEqualTo(3);
        assertThat(estimator.getMessageEstimateCalls()).isEqualTo(callsAfterSeed);
    }

    private static class WordCountTokenCountEstimator implements TokenCountEstimator {

        @Override
        public int estimateTokenCountInText(String text) {
            if (text == null || text.trim().isEmpty()) {
                return 1;
            }
            return text.trim().split("\\s+").length;
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
