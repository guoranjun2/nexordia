package org.freeplane.plugin.ai.chat;

import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.service.memory.ChatMemoryService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;
import org.freeplane.plugin.ai.tools.MessageBuilder;
import org.freeplane.plugin.ai.tools.utilities.ToolCaller;

public class AssistantProfileChatMemory implements ChatMemory {

    private final Object id;
    private final Function<Object, Integer> maxTokensProvider;
    private final ChatTokenEstimator tokenEstimator;
    private GeneralSystemMessage generalSystemMessage;
    private final List<ChatMessage> conversationMessages = new ArrayList<>();
    private final List<ToolCallSummaryRecord> toolCallSummaryRecords = new ArrayList<>();
    private int activeStartIndex;
    private final List<Integer> turnEndIndexes = new ArrayList<>();
    private int currentTurnCount;

    private AssistantProfileChatMemory(Builder builder) {
        this.id = ensureNotNull(builder.id, "id");
        this.maxTokensProvider = ensureNotNull(builder.maxTokensProvider, "maxTokensProvider");
        this.tokenEstimator = new ChatTokenEstimator(builder.tokenEstimatorModelNameProvider);
        ensureGreaterThanZero(this.maxTokensProvider.apply(this.id), "maxTokens");
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public void add(ChatMessage message) {
        if (message == null) {
            return;
        }
        discardRedoBranchIfNeeded();
        if (message instanceof TranscriptHiddenSystemMessage) {
            if (!containsInstructionOfType(TranscriptHiddenSystemMessage.class)) {
                addConversationMessage(message);
                addConversationMessage(new InstructionAckMessage());
                rebuildTurnBoundaries();
            }
            return;
        }
        if (message instanceof RemovedForSpaceSystemMessage) {
            markContextWindowStart();
            return;
        }
        if (message instanceof AssistantProfileControlInstructionMessage) {
            shortenStoredProfileInstructions();
            addConversationMessage(message);
            addConversationMessage(new InstructionAckMessage());
            rebuildTurnBoundaries();
            return;
        }
        if (message instanceof InstructionAckMessage) {
            return;
        }
        if (message instanceof SystemMessage) {
            setGeneralSystemMessage(toGeneralSystemMessage((SystemMessage) message));
            rebuildTurnBoundaries();
            return;
        }
        addConversationMessage(message);
        rebuildTurnBoundaries();
    }

    @Override
    public List<ChatMessage> messages() {
        return buildMessages(activeConversationEndIndex());
    }

    @Override
    public void clear() {
        generalSystemMessage = null;
        conversationMessages.clear();
        toolCallSummaryRecords.clear();
        activeStartIndex = 0;
        turnEndIndexes.clear();
        currentTurnCount = 0;
    }

    public boolean canUndo() {
        return currentTurnCount > firstActiveTurnIndex();
    }

    public int conversationMessageCount() {
        return conversationMessages.size();
    }

    public void truncateConversationMessagesTo(int size) {
        int targetSize = Math.max(0, Math.min(size, conversationMessages.size()));
        while (conversationMessages.size() > targetSize) {
            removeConversationMessage(conversationMessages.size() - 1);
        }
        removeSummariesBeyond(targetSize);
        activeStartIndex = Math.min(activeStartIndex, targetSize);
        rebuildTurnBoundaries();
    }

    public boolean canRedo() {
        return currentTurnCount < turnEndIndexes.size();
    }

    public String undo() {
        if (!canUndo()) {
            return "";
        }
        int firstActive = firstActiveTurnIndex();
        int turnIndex = currentTurnCount - 1;
        int from = turnIndex == 0 ? 0 : turnEndIndexes.get(turnIndex - 1);
        from = Math.max(from, activeStartIndex);
        int to = turnEndIndexes.get(turnIndex);
        currentTurnCount = turnIndex;
        return findUserMessageInRange(from, to);
    }

    public void redo() {
        if (!canRedo()) {
            return;
        }
        currentTurnCount++;
    }

    public void initializeUndoRedoFromMessages() {
        rebuildTurnBoundaries();
    }

    public boolean evictOldestTurn() {
        return advanceWindowByOneTurn();
    }

    public List<ChatTranscriptEntry> activeTranscriptEntries() {
        List<ChatTranscriptEntry> entries = new ArrayList<>();
        int endIndex = activeConversationEndIndex();
        int startIndex = Math.min(activeStartIndex, endIndex);
        for (int index = 0; index < endIndex; index++) {
            if (index == startIndex && startIndex > 0 && endIndex > startIndex) {
                entries.add(new ChatTranscriptEntry(ChatTranscriptRole.REMOVED_FOR_SPACE_SYSTEM,
                    RemovedForSpaceSystemMessage.DEFAULT_TEXT));
            }
            ChatMessage message = conversationMessages.get(index);
            ChatTranscriptEntry entry = toTranscriptEntry(message);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<ChatMessage> activeConversationMessagesForRendering() {
        return buildRawMessages(activeConversationEndIndex());
    }

    public List<ChatMemoryRenderEntry> activeConversationRenderEntries() {
        int endIndex = activeConversationEndIndex();
        if (endIndex == 0) {
            return Collections.emptyList();
        }
        List<ChatMemoryRenderEntry> entries = new ArrayList<>();
        if (generalSystemMessage != null) {
            entries.add(ChatMemoryRenderEntry.forMessage(generalSystemMessage));
        }
        int startIndex = Math.min(activeStartIndex, endIndex);
        List<ToolCallSummaryRecord> orderedSummaries = summariesInRange(0, endIndex);
        int summaryCursor = 0;
        for (int index = 0; index < endIndex; index++) {
            if (index == startIndex && startIndex > 0 && endIndex > startIndex) {
                entries.add(ChatMemoryRenderEntry.forMessage(new RemovedForSpaceSystemMessage()));
            }
            while (summaryCursor < orderedSummaries.size()
                && orderedSummaries.get(summaryCursor).conversationIndex() == index) {
                ToolCallSummaryRecord summary = orderedSummaries.get(summaryCursor);
                entries.add(ChatMemoryRenderEntry.forToolSummary(summary.summaryText(), summary.toolCaller()));
                summaryCursor++;
            }
            entries.add(ChatMemoryRenderEntry.forMessage(conversationMessages.get(index)));
        }
        while (summaryCursor < orderedSummaries.size()) {
            ToolCallSummaryRecord summary = orderedSummaries.get(summaryCursor);
            entries.add(ChatMemoryRenderEntry.forToolSummary(summary.summaryText(), summary.toolCaller()));
            summaryCursor++;
        }
        return entries;
    }

    public void markContextWindowStart() {
        activeStartIndex = Math.max(activeStartIndex, conversationMessages.size());
    }

    public void addToolCallSummary(String summaryText, ToolCaller toolCaller) {
        if (summaryText == null || summaryText.trim().isEmpty()) {
            return;
        }
        toolCallSummaryRecords.add(new ToolCallSummaryRecord(
            conversationMessages.size(),
            summaryText,
            toolCaller));
    }

    ChatUsageTotals estimateTokenUsageForActiveWindow() {
        int endIndex = activeConversationEndIndex();
        int startIndex = Math.min(activeStartIndex, endIndex);
        return estimateTokenUsageForRange(startIndex, endIndex);
    }

    ChatUsageTotals estimateTokenUsageForFullConversation() {
        return estimateTokenUsageForRange(0, conversationMessages.size());
    }

    public boolean onResponseTokenUsage(TokenUsage ignoredUsage) {
        return evictIfNeededAfterResponse();
    }

    private boolean evictIfNeededAfterResponse() {
        int maxTokens = maxTokensProvider.apply(id);
        ensureGreaterThanZero(maxTokens, "maxTokens");
        boolean evicted = false;
        while (estimateTotalTokensForActiveWindow() > maxTokens) {
            if (!canAdvanceWindowWithoutRemovingLastUserMessage()) {
                break;
            }
            if (!advanceWindowByOneTurn()) {
                break;
            }
            evicted = true;
        }
        return evicted;
    }

    private List<ChatMessage> buildMessages(int conversationEndIndex) {
        List<ChatMessage> messages = new ArrayList<>();
        if (generalSystemMessage != null) {
            messages.add(generalSystemMessage);
        }
        int endIndex = Math.max(0, Math.min(conversationEndIndex, conversationMessages.size()));
        int startIndex = Math.min(activeStartIndex, endIndex);
        for (int index = startIndex; index < endIndex; index++) {
            ChatMessage message = conversationMessages.get(index);
            if (message instanceof AssistantProfileControlInstructionMessage) {
                messages.add(MessageBuilder.buildSystemInstructionUserMessage(
                    ((AssistantProfileControlInstructionMessage) message).text()));
                continue;
            }
            if (message instanceof TranscriptHiddenSystemMessage
                || message instanceof RemovedForSpaceSystemMessage) {
                messages.add(MessageBuilder.buildSystemInstructionUserMessage(
                    ((SystemMessage) message).text()));
                continue;
            }
            messages.add(message);
        }
        return messages;
    }

    private List<ChatMessage> buildRawMessages(int conversationEndIndex) {
        List<ChatMessage> messages = new ArrayList<>();
        if (generalSystemMessage != null) {
            messages.add(generalSystemMessage);
        }
        int endIndex = Math.max(0, Math.min(conversationEndIndex, conversationMessages.size()));
        for (int index = 0; index < endIndex; index++) {
            messages.add(conversationMessages.get(index));
        }
        return messages;
    }

    private int activeConversationEndIndex() {
        if (canRedo()) {
            int firstActive = firstActiveTurnIndex();
            if (currentTurnCount <= firstActive) {
                return activeStartIndex;
            }
            return turnEndIndexes.get(currentTurnCount - 1);
        }
        return conversationMessages.size();
    }

    private void shortenStoredProfileInstructions() {
        for (int index = 0; index < conversationMessages.size(); index++) {
            ChatMessage message = conversationMessages.get(index);
            if (message instanceof AssistantProfileControlInstructionMessage) {
                AssistantProfileControlInstructionMessage profileMessage = (AssistantProfileControlInstructionMessage) message;
                replaceConversationMessage(index, profileMessage.withoutProfileDefinition());
            }
        }
    }

    private boolean containsInstructionOfType(Class<? extends SystemMessage> messageClass) {
        for (ChatMessage message : conversationMessages) {
            if (messageClass.isInstance(message)) {
                return true;
            }
        }
        return false;
    }

    private GeneralSystemMessage toGeneralSystemMessage(SystemMessage message) {
        if (message instanceof GeneralSystemMessage) {
            return (GeneralSystemMessage) message;
        }
        return new GeneralSystemMessage(message.text());
    }

    private void rebuildTurnBoundaries() {
        turnEndIndexes.clear();
        for (int index = 0; index < conversationMessages.size(); index++) {
            ChatMessage message = conversationMessages.get(index);
            if (!(message instanceof AiMessage) || message instanceof InstructionAckMessage) {
                continue;
            }
            AiMessage aiMessage = (AiMessage) message;
            if (!aiMessage.hasToolExecutionRequests()) {
                turnEndIndexes.add(index + 1);
            }
        }
        currentTurnCount = turnEndIndexes.size();
        int endIndex = activeConversationEndIndex();
        if (activeStartIndex > endIndex) {
            activeStartIndex = endIndex;
        }
    }

    private void discardRedoBranchIfNeeded() {
        if (!canRedo()) {
            return;
        }
        int keepSize = currentTurnCount == 0 ? 0 : turnEndIndexes.get(currentTurnCount - 1);
        while (conversationMessages.size() > keepSize) {
            removeConversationMessage(conversationMessages.size() - 1);
        }
        while (turnEndIndexes.size() > currentTurnCount) {
            turnEndIndexes.remove(turnEndIndexes.size() - 1);
        }
        activeStartIndex = Math.min(activeStartIndex, keepSize);
    }

    private int firstActiveTurnIndex() {
        int startIndex = Math.min(activeStartIndex, conversationMessages.size());
        for (int index = 0; index < turnEndIndexes.size(); index++) {
            int turnEnd = turnEndIndexes.get(index);
            if (turnEnd > startIndex) {
                return index;
            }
        }
        return turnEndIndexes.size();
    }

    private String findUserMessageInRange(int from, int to) {
        int safeFrom = Math.max(0, from);
        int safeTo = Math.min(to, conversationMessages.size());
        for (int index = safeTo - 1; index >= safeFrom; index--) {
            ChatMessage message = conversationMessages.get(index);
            if (message instanceof UserMessage) {
                String text = ((UserMessage) message).singleText();
                if (text != null && !text.startsWith(MessageBuilder.CONTROL_INSTRUCTION_PREFIX)) {
                    return text;
                }
            }
        }
        return "";
    }

    private ChatTranscriptEntry toTranscriptEntry(ChatMessage message) {
        if (message == null) {
            return null;
        }
        if (message instanceof AssistantProfileControlInstructionMessage) {
            AssistantProfileControlInstructionMessage profileMessage = (AssistantProfileControlInstructionMessage) message;
            return new AssistantProfileTranscriptEntry(
                profileMessage.getProfileId(),
                profileMessage.getProfileName(),
                profileMessage.containsProfileDefinition());
        }
        if (message instanceof RemovedForSpaceSystemMessage) {
            return new ChatTranscriptEntry(ChatTranscriptRole.REMOVED_FOR_SPACE_SYSTEM,
                ((RemovedForSpaceSystemMessage) message).text());
        }
        if (message instanceof UserMessage) {
            String text = ((UserMessage) message).singleText();
            if (text == null || text.trim().isEmpty() || text.startsWith(MessageBuilder.CONTROL_INSTRUCTION_PREFIX)) {
                return null;
            }
            return new ChatTranscriptEntry(ChatTranscriptRole.USER, text);
        }
        if (message instanceof AiMessage && !(message instanceof InstructionAckMessage)) {
            String text = ((AiMessage) message).text();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            return new ChatTranscriptEntry(ChatTranscriptRole.ASSISTANT, text);
        }
        return null;
    }

    private void setGeneralSystemMessage(GeneralSystemMessage message) {
        generalSystemMessage = message;
    }

    private void addConversationMessage(ChatMessage message) {
        addConversationMessage(conversationMessages.size(), message);
    }

    private void addConversationMessage(int index, ChatMessage message) {
        shiftSummariesForInsert(index);
        conversationMessages.add(index, message);
    }

    private ChatMessage removeConversationMessage(int index) {
        shiftSummariesForRemove(index);
        return conversationMessages.remove(index);
    }

    private void replaceConversationMessage(int index, ChatMessage message) {
        conversationMessages.set(index, message);
    }

    private void shiftSummariesForInsert(int insertIndex) {
        for (int index = 0; index < toolCallSummaryRecords.size(); index++) {
            toolCallSummaryRecords.get(index).shiftForInsert(insertIndex);
        }
    }

    private void shiftSummariesForRemove(int removeIndex) {
        for (int index = 0; index < toolCallSummaryRecords.size(); index++) {
            toolCallSummaryRecords.get(index).shiftForRemove(removeIndex);
        }
    }

    private void removeSummariesBeyond(int targetSize) {
        toolCallSummaryRecords.removeIf(summary -> summary.conversationIndex() >= targetSize);
    }

    private List<ToolCallSummaryRecord> summariesInRange(int startIndex, int conversationEndIndex) {
        if (toolCallSummaryRecords.isEmpty()) {
            return Collections.emptyList();
        }
        List<ToolCallSummaryRecord> summaries = new ArrayList<>();
        for (int index = 0; index < toolCallSummaryRecords.size(); index++) {
            ToolCallSummaryRecord summary = toolCallSummaryRecords.get(index);
            if (summary.conversationIndex() >= startIndex && summary.conversationIndex() <= conversationEndIndex) {
                summaries.add(summary);
            }
        }
        return summaries;
    }

    private boolean advanceWindowByOneTurn() {
        rebuildTurnBoundaries();
        int endIndex = activeConversationEndIndex();
        int startIndex = Math.min(activeStartIndex, endIndex);
        int nextTurnEnd = findNextTurnEndAfter(startIndex);
        if (nextTurnEnd <= startIndex) {
            return false;
        }
        activeStartIndex = nextTurnEnd;
        rebuildTurnBoundaries();
        return true;
    }

    private boolean canAdvanceWindowWithoutRemovingLastUserMessage() {
        int endIndex = activeConversationEndIndex();
        int startIndex = Math.min(activeStartIndex, endIndex);
        int nextTurnEnd = findNextTurnEndAfter(startIndex);
        if (nextTurnEnd <= startIndex) {
            return false;
        }
        for (int index = nextTurnEnd; index < endIndex; index++) {
            ChatMessage message = conversationMessages.get(index);
            if (message instanceof UserMessage) {
                return true;
            }
        }
        return false;
    }

    private int findNextTurnEndAfter(int startIndex) {
        for (int index = 0; index < turnEndIndexes.size(); index++) {
            int turnEnd = turnEndIndexes.get(index);
            if (turnEnd > startIndex) {
                return turnEnd;
            }
        }
        return -1;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AssistantProfileChatMemory withMaxTokens(int maxTokens) {
        return builder().maxTokens(maxTokens).build();
    }

    public static class Builder {

        private Object id = ChatMemoryService.DEFAULT;
        private Function<Object, Integer> maxTokensProvider;
        private Supplier<String> tokenEstimatorModelNameProvider = () -> null;

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokensProvider = ignored -> maxTokens;
            return this;
        }

        public Builder dynamicMaxTokens(Function<Object, Integer> maxTokensProvider) {
            this.maxTokensProvider = maxTokensProvider;
            return this;
        }

        public Builder tokenEstimatorModelNameProvider(Supplier<String> tokenEstimatorModelNameProvider) {
            this.tokenEstimatorModelNameProvider = tokenEstimatorModelNameProvider;
            return this;
        }

        public AssistantProfileChatMemory build() {
            return new AssistantProfileChatMemory(this);
        }
    }

    private ChatUsageTotals estimateTokenUsageForRange(int startIndex, int endIndex) {
        long inputTokens = 0L;
        long outputTokens = 0L;
        int safeStart = Math.max(0, startIndex);
        int safeEnd = Math.min(endIndex, conversationMessages.size());
        for (int index = safeStart; index < safeEnd; index++) {
            ChatMessage message = conversationMessages.get(index);
            if (!isRemovableMessage(message)) {
                continue;
            }
            int tokenCount = tokenEstimator.estimateTokenCountInMessage(message);
            if (message instanceof AiMessage) {
                outputTokens += tokenCount;
            } else {
                inputTokens += tokenCount;
            }
        }
        return ChatUsageTotals.estimated(inputTokens, outputTokens);
    }

    private long estimateTotalTokensForActiveWindow() {
        ChatUsageTotals totals = estimateTokenUsageForActiveWindow();
        return totals.getInputTokenCount() + totals.getOutputTokenCount();
    }

    private boolean isRemovableMessage(ChatMessage message) {
        if (message == null) {
            return false;
        }
        if (message instanceof AssistantProfileControlInstructionMessage
            || message instanceof InstructionAckMessage
            || message instanceof TranscriptHiddenSystemMessage
            || message instanceof RemovedForSpaceSystemMessage
            || message instanceof GeneralSystemMessage) {
            return false;
        }
        if (message instanceof SystemMessage) {
            return false;
        }
        return message instanceof UserMessage
            || message instanceof AiMessage
            || message instanceof ToolExecutionResultMessage;
    }

    private static class ChatTokenEstimator {
        private static final String FALLBACK_MODEL_NAME = "gpt-4o-mini";

        private final Supplier<String> modelNameProvider;
        private OpenAiTokenCountEstimator estimator;
        private String activeModelName;

        private ChatTokenEstimator(Supplier<String> modelNameProvider) {
            this.modelNameProvider = modelNameProvider == null ? () -> null : modelNameProvider;
        }

        int estimateTokenCountInMessage(ChatMessage message) {
            OpenAiTokenCountEstimator activeEstimator = estimator();
            try {
                return activeEstimator.estimateTokenCountInMessage(message);
            } catch (RuntimeException error) {
                return 0;
            }
        }

        private OpenAiTokenCountEstimator estimator() {
            String modelName = normalizeModelName(modelNameProvider.get());
            if (estimator == null || !modelName.equals(activeModelName)) {
                estimator = buildEstimator(modelName);
                activeModelName = modelName;
            }
            return estimator;
        }

        private OpenAiTokenCountEstimator buildEstimator(String modelName) {
            try {
                return new OpenAiTokenCountEstimator(modelName);
            } catch (IllegalArgumentException error) {
                return new OpenAiTokenCountEstimator(FALLBACK_MODEL_NAME);
            }
        }

        private String normalizeModelName(String modelName) {
            if (modelName == null || modelName.trim().isEmpty()) {
                return FALLBACK_MODEL_NAME;
            }
            String normalized = modelName.trim();
            int slashIndex = normalized.lastIndexOf('/');
            if (slashIndex >= 0 && slashIndex < normalized.length() - 1) {
                normalized = normalized.substring(slashIndex + 1);
            }
            return normalized;
        }
    }
}
