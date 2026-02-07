package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.output.TokenUsage;
import java.util.List;
import org.junit.Test;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.tools.MessageBuilder;
import org.freeplane.plugin.ai.tools.utilities.ToolCaller;

public class AssistantProfileChatMemoryTest {

    @Test
    public void messages_ordersSystemMessagesBySlot() {
        AssistantProfileChatMemory uut = createMemory(500);

        uut.add(UserMessage.from("hello"));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(new AssistantProfileControlInstructionMessage("profile", "profile", "", true));
        uut.add(new GeneralSystemMessage("general"));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages).hasSize(6);
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
    }

    @Test
    public void capacity_excludesTranscriptHiddenAndRemovedForSpace() {
        int maxTokens = estimateTokens(
            UserMessage.from("second"),
            AiMessage.from("second answer"));
        AssistantProfileChatMemory uut = createMemory(maxTokens);

        uut.add(new GeneralSystemMessage("general"));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(UserMessage.from("first"));
        uut.add(AiMessage.from("first answer"));
        uut.add(UserMessage.from("second"));
        uut.add(AiMessage.from("second answer"));
        uut.onResponseTokenUsage(new TokenUsage(1, 1));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages.get(0)).isInstanceOf(GeneralSystemMessage.class);
        assertThat(messages)
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .doesNotContain(MessageBuilder.CONTROL_INSTRUCTION_PREFIX + "hidden")
            .doesNotContain("first")
            .contains("second");
        assertThat(messages)
            .extracting(message -> message instanceof AiMessage ? ((AiMessage) message).text() : null)
            .contains("second answer");
    }

    @Test
    public void assistantProfileMessagesDropWhenNoConversationMessagesRemain() {
        int maxTokens = Math.max(1, estimateTokens(
            UserMessage.from("first"),
            AiMessage.from("answer")) - 1);
        AssistantProfileChatMemory uut = createMemory(maxTokens);

        uut.add(new AssistantProfileControlInstructionMessage("profile", "profile",
            "word word word word word word word word word word "
                + "word word word word word word word word word word "
                + "word word word word word word word word word word ",
            true));
        uut.add(UserMessage.from("first"));
        uut.add(AiMessage.from("answer"));
        uut.onResponseTokenUsage(new TokenUsage(1, 1));

        List<ChatMessage> messages = uut.messages();
        assertThat(messages)
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("first");
    }

    @Test
    public void contextBoundaryMarkerInsertedOnceWhenWindowMoves() {
        int maxTokens = estimateTokens(
            UserMessage.from("next"),
            AiMessage.from("third"));
        AssistantProfileChatMemory uut = createMemory(maxTokens);

        uut.add(UserMessage.from("first first first first first first first first first first "
            + "first first first first first first first first first first first first first first first first"));
        uut.add(AiMessage.from("second"));
        uut.add(UserMessage.from("next"));
        uut.add(AiMessage.from("third"));
        uut.onResponseTokenUsage(new TokenUsage(1, 1));

        List<ChatMemoryRenderEntry> entries = uut.activeConversationRenderEntries();
        long markerCount = entries.stream()
            .filter(ChatMemoryRenderEntry::isToolSummary)
            .count();
        long boundaryCount = entries.stream()
            .filter(entry -> entry.chatMessage() instanceof RemovedForSpaceSystemMessage)
            .count();
        assertThat(markerCount).isZero();
        assertThat(boundaryCount).isEqualTo(1);
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
    public void estimateTokenUsageExcludesControlMessages() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(new AssistantProfileControlInstructionMessage("p1", "Profile", "definition", true));
        uut.add(new TranscriptHiddenSystemMessage("hidden"));
        uut.add(UserMessage.from("hello"));
        uut.add(AiMessage.from("response"));

        ChatUsageTotals totals = uut.estimateTokenUsageForActiveWindow();

        int expected = estimateTokens(
            UserMessage.from("hello"),
            AiMessage.from("response"));
        assertThat(totals.getInputTokenCount() + totals.getOutputTokenCount())
            .isEqualTo(expected);
    }

    @Test
    public void evictionKeepsLastUserMessageEvenWhenOverLimit() {
        AssistantProfileChatMemory uut = createMemory(1);
        uut.add(UserMessage.from("first"));
        uut.add(AiMessage.from("answer"));

        boolean evicted = uut.onResponseTokenUsage(new TokenUsage(1, 1));

        assertThat(evicted).isFalse();
        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("first");
    }

    @Test
    public void evictingToolRequestAlsoEvictsToolResults() {
        AssistantProfileChatMemory uut = createMemory(500);
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
            .id("tool-1")
            .name("test")
            .arguments("{}")
            .build();

        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from(List.of(toolRequest)));
        uut.add(ToolExecutionResultMessage.from("tool-1", "test", "result"));
        uut.add(AiMessage.from("done"));

        assertThat(uut.evictOldestTurn()).isTrue();

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
    public void noEvictionOccursWithoutResponseUsage() {
        AssistantProfileChatMemory uut = createMemory(10);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u1", "u2");
        assertThat(uut.activeConversationRenderEntries())
            .noneMatch(entry -> entry.chatMessage() instanceof RemovedForSpaceSystemMessage);
    }

    @Test
    public void evictingAdvancesUntilWithinLimit() {
        int maxTokens = estimateTokens(
            UserMessage.from("u3"),
            AiMessage.from("a3"));
        AssistantProfileChatMemory uut = createMemory(maxTokens);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));
        uut.add(UserMessage.from("u3"));
        uut.add(AiMessage.from("a3"));

        uut.onResponseTokenUsage(new TokenUsage(1, 1));

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u3")
            .doesNotContain("u1", "u2");
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
    public void undoIgnoresToolSummaryMessagesWhenRestoringUserInput() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("user question"));
        uut.addToolCallSummary("searchNodes: query=\"x\"", ToolCaller.CHAT);
        uut.add(AiMessage.from("assistant answer"));

        String restoredUserInput = uut.undo();

        assertThat(restoredUserInput).isEqualTo("user question");
    }

    @Test
    public void undoSingleTurnLeavesRenderEntriesEmpty() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(new AssistantProfileControlInstructionMessage("profile", "Profile", "definition", true));
        uut.add(UserMessage.from("hello"));
        uut.add(AiMessage.from("answer"));

        uut.undo();

        assertThat(uut.activeConversationRenderEntries()).isEmpty();
    }

    @Test
    public void undoTwoTurnsOutOfThreeKeepsFirstTurnVisible() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));
        uut.add(UserMessage.from("u3"));
        uut.add(AiMessage.from("a3"));

        uut.undo();
        uut.undo();

        assertThat(uut.activeConversationRenderEntries())
            .extracting(entry -> entry.chatMessage() instanceof UserMessage
                ? ((UserMessage) entry.chatMessage()).singleText()
                : null)
            .contains("u1")
            .doesNotContain("u2", "u3");
    }

    @Test
    public void undoTreatsToolRequestResultAndFinalAssistantAsSingleTurn() {
        AssistantProfileChatMemory uut = createMemory(500);
        ToolExecutionRequest toolRequest = ToolExecutionRequest.builder()
            .id("tool-1")
            .name("searchNodes")
            .arguments("{\"request\":{\"query\":\"root\"}}")
            .build();
        uut.add(UserMessage.from("what is root"));
        uut.add(AiMessage.from(List.of(toolRequest)));
        uut.add(ToolExecutionResultMessage.from("tool-1", "searchNodes", "Root"));
        uut.add(AiMessage.from("Root is Spec-driven development"));

        String restoredUserInput = uut.undo();

        assertThat(restoredUserInput).isEqualTo("what is root");
        assertThat(uut.activeConversationRenderEntries()).isEmpty();
    }

    @Test
    public void undoKeepsPreviousToolSummaryAndHidesUndoneSummary() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.addToolCallSummary("summary-1", ToolCaller.CHAT);
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));
        uut.addToolCallSummary("summary-2", ToolCaller.CHAT);

        uut.undo();

        assertThat(uut.activeConversationRenderEntries())
            .filteredOn(ChatMemoryRenderEntry::isToolSummary)
            .extracting(ChatMemoryRenderEntry::toolSummaryText)
            .contains("summary-1")
            .doesNotContain("summary-2");
    }

    @Test
    public void redoRestoresToolSummaryForRedoneTurn() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.addToolCallSummary("summary-1", ToolCaller.CHAT);
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));
        uut.addToolCallSummary("summary-2", ToolCaller.CHAT);

        uut.undo();
        uut.redo();

        assertThat(uut.activeConversationRenderEntries())
            .filteredOn(ChatMemoryRenderEntry::isToolSummary)
            .extracting(ChatMemoryRenderEntry::toolSummaryText)
            .contains("summary-1", "summary-2");
    }

    @Test
    public void undoRemovesProfileInstructionWhenItBelongsToOnlyTurn() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(new AssistantProfileControlInstructionMessage("p1", "A sayer", "profile text", true));
        uut.add(UserMessage.from("hi"));
        uut.add(AiMessage.from("hello"));

        uut.undo();

        assertThat(uut.activeConversationRenderEntries()).isEmpty();
    }

    @Test
    public void recordTokenUsageEvictsOldestTurnAfterResponse() {
        int maxTokens = estimateTokens(
            UserMessage.from("u2"),
            AiMessage.from("a2"));
        AssistantProfileChatMemory uut = createMemory(maxTokens);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));

        uut.onResponseTokenUsage(new TokenUsage(1, 1));

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .doesNotContain("u1")
            .contains("u2");
        assertThat(uut.activeConversationRenderEntries())
            .anyMatch(entry -> entry.chatMessage() instanceof RemovedForSpaceSystemMessage);
    }

    @Test
    public void recordTokenUsageKeepsWindowWhenWithinLimit() {
        int maxTokens = estimateTokens(
            UserMessage.from("u1"),
            AiMessage.from("a1"),
            UserMessage.from("u2"),
            AiMessage.from("a2")) + 10;
        AssistantProfileChatMemory uut = createMemory(maxTokens);
        uut.add(UserMessage.from("u1"));
        uut.add(AiMessage.from("a1"));
        uut.add(UserMessage.from("u2"));
        uut.add(AiMessage.from("a2"));

        uut.onResponseTokenUsage(new TokenUsage(1, 1));

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .contains("u1", "u2");
        assertThat(uut.activeConversationRenderEntries())
            .noneMatch(entry -> entry.chatMessage() instanceof RemovedForSpaceSystemMessage);
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

    @Test
    public void messagesExcludeEvictedOldestTurnContent() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("first question"));
        uut.add(AiMessage.from("first answer"));
        uut.add(UserMessage.from("second question"));
        uut.add(AiMessage.from("second answer"));
        assertThat(uut.evictOldestTurn()).isTrue();

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .doesNotContain("first question")
            .contains("second question");
    }

    @Test
    public void evictedOldestTurnDoesNotReturnAfterUndoRedo() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("first question"));
        uut.add(AiMessage.from("first answer"));
        uut.add(UserMessage.from("second question"));
        uut.add(AiMessage.from("second answer"));
        assertThat(uut.evictOldestTurn()).isTrue();

        assertThat(uut.canUndo()).isTrue();
        uut.undo();
        assertThat(uut.canRedo()).isTrue();
        uut.redo();

        assertThat(uut.messages())
            .extracting(message -> message instanceof UserMessage ? ((UserMessage) message).singleText() : null)
            .doesNotContain("first question")
            .contains("second question");
    }

    @Test
    public void transcriptEntriesExcludeEvictedOldestTurnContent() {
        AssistantProfileChatMemory uut = createMemory(500);
        uut.add(UserMessage.from("first question"));
        uut.add(AiMessage.from("first answer"));
        uut.add(UserMessage.from("second question"));
        uut.add(AiMessage.from("second answer"));
        assertThat(uut.evictOldestTurn()).isTrue();

        assertThat(uut.activeTranscriptEntries())
            .extracting(entry -> entry.getRole().name() + ":" + entry.getText())
            .anyMatch(value -> value.contains("REMOVED_FOR_SPACE_SYSTEM:" + RemovedForSpaceSystemMessage.DEFAULT_TEXT))
            .anyMatch(value -> value.contains("first question"))
            .anyMatch(value -> value.contains("first answer"))
            .anyMatch(value -> value.contains("second question"))
            .anyMatch(value -> value.contains("second answer"));
    }

    private AssistantProfileChatMemory createMemory(int maxTokens) {
        return AssistantProfileChatMemory.builder()
            .maxTokens(maxTokens)
            .tokenEstimatorModelNameProvider(() -> "gpt-4o-mini")
            .build();
    }

    private int estimateTokens(ChatMessage... messages) {
        OpenAiTokenCountEstimator estimator = new OpenAiTokenCountEstimator("gpt-4o-mini");
        int total = 0;
        for (ChatMessage message : messages) {
            total += estimator.estimateTokenCountInMessage(message);
        }
        return total;
    }
}
