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
    private ProfileInstructionFactory profileInstructionFactory;
    private GeneralSystemMessage generalSystemMessage;
    private final List<ChatMessage> conversationMessages = new ArrayList<>();
    private int activeStartIndex;
    private final List<Integer> turnEndIndexes = new ArrayList<>();
    private int currentTurnCount;

    private AssistantProfileChatMemory(Builder builder) {
        this.id = ensureNotNull(builder.id, "id");
        this.maxTokensProvider = ensureNotNull(builder.maxTokensProvider, "maxTokensProvider");
        this.tokenEstimator = new ChatTokenEstimator(builder.tokenEstimatorModelNameProvider);
        this.profileInstructionFactory = resolveProfileInstructionFactory(builder.profileInstructionFactory);
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
        if (message instanceof AssistantProfileSwitchMessage) {
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
        rebalanceActiveWindowForCurrentTurnRange();
        return findUserMessageInRange(from, to);
    }

    public void redo() {
        if (!canRedo()) {
            return;
        }
        currentTurnCount++;
        rebalanceActiveWindowForCurrentTurnRange();
    }

    public void initializeUndoRedoFromMessages() {
        rebuildTurnBoundaries();
    }

    void expandWindowAfterTranscriptRestoreIfUnderutilized() {
        rebuildTurnBoundaries();
        int endIndex = activeConversationEndIndex();
        if (endIndex <= 0) {
            return;
        }
        int maxTokens = maxTokensProvider.apply(id);
        ensureGreaterThanZero(maxTokens, "maxTokens");
        int startIndex = Math.min(activeStartIndex, endIndex);
        long activeTokens = estimateTotalTokensForRange(startIndex, endIndex);
        if (activeTokens >= maxTokens) {
            return;
        }
        int selectedStart = startIndex;
        while (true) {
            int previousTurnStart = previousTurnStartFor(selectedStart);
            if (previousTurnStart < 0) {
                break;
            }
            long expandedTokens = estimateTotalTokensForRange(previousTurnStart, endIndex);
            if (expandedTokens > maxTokens) {
                break;
            }
            selectedStart = previousTurnStart;
            if (expandedTokens >= maxTokens) {
                break;
            }
        }
        activeStartIndex = selectedStart;
    }

    public boolean evictOldestTurn() {
        rebuildTurnBoundaries();
        if (!canAdvanceWindowByTurnWithMinimumRetention(1)) {
            return false;
        }
        return advanceWindowByOneTurn();
    }

    public List<ChatTranscriptEntry> transcriptEntriesForPersistence() {
        List<ChatTranscriptEntry> entries = new ArrayList<>();
        int endIndex = activeConversationEndIndex();
        int startIndex = Math.min(activeStartIndex, endIndex);
        if (startIndex > 0) {
            startIndex = alignVisibleStartIndex(startIndex, endIndex);
        }
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
        return buildRenderEntries(false);
    }

    public List<ChatMemoryRenderEntry> panelConversationRenderEntries() {
        return buildRenderEntries(true);
    }

    private List<ChatMemoryRenderEntry> buildRenderEntries(boolean includeMessagesBeforeActiveWindow) {
        int endIndex = activeConversationEndIndex();
        if (endIndex == 0) {
            return Collections.emptyList();
        }
        List<ChatMemoryRenderEntry> entries = new ArrayList<>();
        if (generalSystemMessage != null) {
            entries.add(ChatMemoryRenderEntry.forMessage(generalSystemMessage));
        }
        int startIndex = Math.min(activeStartIndex, endIndex);
        if (startIndex > 0) {
            startIndex = alignVisibleStartIndex(startIndex, endIndex);
        }
        int firstRenderedIndex = includeMessagesBeforeActiveWindow ? 0 : startIndex;
        for (int index = firstRenderedIndex; index < endIndex; index++) {
            if (index == startIndex && startIndex > 0 && endIndex > startIndex) {
                entries.add(ChatMemoryRenderEntry.forMessage(new RemovedForSpaceSystemMessage()));
            }
            ChatMessage message = conversationMessages.get(index);
            if (message instanceof ToolCallSummaryMessage) {
                ToolCallSummaryMessage summaryMessage = (ToolCallSummaryMessage) message;
                entries.add(ChatMemoryRenderEntry.forToolSummary(summaryMessage.text(), summaryMessage.toolCaller()));
                continue;
            }
            entries.add(ChatMemoryRenderEntry.forMessage(message));
        }
        return entries;
    }

    public void markContextWindowStart() {
        activeStartIndex = Math.max(activeStartIndex, conversationMessages.size());
    }

    void addToolCallSummary(String summaryText, ToolCaller toolCaller) {
        if (summaryText == null || summaryText.trim().isEmpty()) {
            return;
        }
        conversationMessages.add(new ToolCallSummaryMessage(summaryText, toolCaller));
        rebuildTurnBoundaries();
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

    void setProfileInstructionFactory(ProfileInstructionFactory profileInstructionFactory) {
        this.profileInstructionFactory = resolveProfileInstructionFactory(profileInstructionFactory);
    }

    private boolean evictIfNeededAfterResponse() {
        int maxTokens = maxTokensProvider.apply(id);
        ensureGreaterThanZero(maxTokens, "maxTokens");
        long estimatedTokens = estimateTotalTokensForActiveWindow();
        if (estimatedTokens < maxTokens) {
            return false;
        }
        int resetTargetTokens = maxTokens / 4;
        int minimumTurnBlocksToKeep = minimumTurnBlocksToKeep(maxTokens);
        boolean evicted = false;
        while (estimateTotalTokensForActiveWindow() > resetTargetTokens) {
            if (!canAdvanceWindowByTurnWithMinimumRetention(minimumTurnBlocksToKeep)) {
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
        int latestProfileSwitchIndex = findLatestProfileSwitchIndex(endIndex);
        UserMessage latestProfileInstruction = buildProfileInstructionForIndex(latestProfileSwitchIndex);
        if (latestProfileInstruction != null && latestProfileSwitchIndex >= 0
            && latestProfileSwitchIndex < startIndex) {
            messages.add(latestProfileInstruction);
        }
        for (int index = startIndex; index < endIndex; index++) {
            ChatMessage message = conversationMessages.get(index);
            if (message instanceof AssistantProfileSwitchMessage) {
                if (index == latestProfileSwitchIndex && latestProfileInstruction != null) {
                    messages.add(latestProfileInstruction);
                }
                continue;
            }
            if (message instanceof ToolCallSummaryMessage) {
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

    private int conversationEndIndexForCurrentTurnRange() {
        if (canRedo()) {
            if (currentTurnCount <= 0) {
                return 0;
            }
            return turnEndIndexes.get(currentTurnCount - 1);
        }
        return conversationMessages.size();
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
        if (message instanceof AssistantProfileSwitchMessage) {
            AssistantProfileSwitchMessage profileMessage = (AssistantProfileSwitchMessage) message;
            return new AssistantProfileTranscriptEntry(
                profileMessage.getProfileId(),
                profileMessage.getProfileName(),
                false);
        }
        if (message instanceof RemovedForSpaceSystemMessage) {
            return new ChatTranscriptEntry(ChatTranscriptRole.REMOVED_FOR_SPACE_SYSTEM,
                ((RemovedForSpaceSystemMessage) message).text());
        }
        if (message instanceof ToolCallSummaryMessage) {
            return null;
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
        conversationMessages.add(message);
    }

    private ChatMessage removeConversationMessage(int index) {
        return conversationMessages.remove(index);
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

    private void rebalanceActiveWindowForCurrentTurnRange() {
        int maxTokens = maxTokensProvider.apply(id);
        ensureGreaterThanZero(maxTokens, "maxTokens");
        int endIndex = conversationEndIndexForCurrentTurnRange();
        if (endIndex <= 0 || currentTurnCount <= 0) {
            activeStartIndex = 0;
            return;
        }
        int selectedStart = turnStartIndex(currentTurnCount - 1);
        for (int turnIndex = currentTurnCount - 2; turnIndex >= 0; turnIndex--) {
            int candidateStart = turnStartIndex(turnIndex);
            if (estimateTotalTokensForRange(candidateStart, endIndex) <= maxTokens) {
                selectedStart = candidateStart;
                continue;
            }
            break;
        }
        activeStartIndex = selectedStart;
    }

    private int turnStartIndex(int turnIndex) {
        if (turnIndex <= 0) {
            return 0;
        }
        return turnEndIndexes.get(turnIndex - 1);
    }

    private int previousTurnStartFor(int startIndex) {
        int safeStart = Math.max(0, startIndex);
        int previousTurnIndex = -1;
        for (int index = 0; index < turnEndIndexes.size(); index++) {
            int turnEnd = turnEndIndexes.get(index);
            if (turnEnd <= safeStart) {
                previousTurnIndex = index;
                continue;
            }
            break;
        }
        if (previousTurnIndex < 0) {
            return -1;
        }
        return turnStartIndex(previousTurnIndex);
    }

    private boolean canAdvanceWindowByTurnWithMinimumRetention(int minimumTurnBlocksToKeep) {
        return activeTurnRanges().size() > minimumTurnBlocksToKeep;
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

    private int alignVisibleStartIndex(int startIndex, int endIndex) {
        int alignedStart = Math.max(0, Math.min(startIndex, endIndex));
        while (alignedStart < endIndex) {
            ChatMessage message = conversationMessages.get(alignedStart);
            if (message instanceof ToolCallSummaryMessage) {
                alignedStart++;
                continue;
            }
            break;
        }
        return alignedStart;
    }

    private int findLatestProfileSwitchIndex(int endIndex) {
        for (int index = endIndex - 1; index >= 0; index--) {
            if (conversationMessages.get(index) instanceof AssistantProfileSwitchMessage) {
                return index;
            }
        }
        return -1;
    }

    private UserMessage buildProfileInstructionForIndex(int messageIndex) {
        if (messageIndex < 0 || messageIndex >= conversationMessages.size()) {
            return null;
        }
        ChatMessage message = conversationMessages.get(messageIndex);
        if (!(message instanceof AssistantProfileSwitchMessage)) {
            return null;
        }
        AssistantProfileInstructionMessage profileInstruction =
            profileInstructionFactory.buildFor((AssistantProfileSwitchMessage) message);
        if (profileInstruction == null) {
            return null;
        }
        return MessageBuilder.buildSystemInstructionUserMessage(profileInstruction.singleText());
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
        private ProfileInstructionFactory profileInstructionFactory;

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

        public Builder profileInstructionFactory(ProfileInstructionFactory profileInstructionFactory) {
            this.profileInstructionFactory = profileInstructionFactory;
            return this;
        }

        public AssistantProfileChatMemory build() {
            return new AssistantProfileChatMemory(this);
        }
    }

    interface ProfileInstructionFactory {
        AssistantProfileInstructionMessage buildFor(AssistantProfileSwitchMessage profileSwitchMessage);
    }

    private ProfileInstructionFactory resolveProfileInstructionFactory(ProfileInstructionFactory profileInstructionFactory) {
        if (profileInstructionFactory != null) {
            return profileInstructionFactory;
        }
        return profileSwitchMessage -> {
            if (profileSwitchMessage == null) {
                return null;
            }
            return new AssistantProfileInstructionMessage(
                profileSwitchMessage.getProfileId(),
                profileSwitchMessage.getProfileName(),
                "");
        };
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

    private long estimateTotalTokensForRange(int startIndex, int endIndex) {
        ChatUsageTotals totals = estimateTokenUsageForRange(startIndex, endIndex);
        return totals.getInputTokenCount() + totals.getOutputTokenCount();
    }

    private int minimumTurnBlocksToKeep(int maxTokens) {
        List<ActiveTurnRange> ranges = activeTurnRanges();
        if (ranges.size() <= 1) {
            return 1;
        }
        ActiveTurnRange secondLast = ranges.get(ranges.size() - 2);
        ActiveTurnRange last = ranges.get(ranges.size() - 1);
        long twoTurnTokenCount = estimateTotalTokensForRange(secondLast.startIndex, last.endIndex);
        return twoTurnTokenCount <= maxTokens ? 2 : 1;
    }

    private List<ActiveTurnRange> activeTurnRanges() {
        List<ActiveTurnRange> ranges = new ArrayList<>();
        int endIndex = activeConversationEndIndex();
        int startIndex = Math.min(activeStartIndex, endIndex);
        int previousEnd = 0;
        for (int index = 0; index < turnEndIndexes.size(); index++) {
            int turnEnd = turnEndIndexes.get(index);
            int turnStart = previousEnd;
            previousEnd = turnEnd;
            if (turnEnd <= startIndex) {
                continue;
            }
            int rangeStart = Math.max(turnStart, startIndex);
            int rangeEnd = Math.min(turnEnd, endIndex);
            if (rangeEnd > rangeStart) {
                ranges.add(new ActiveTurnRange(rangeStart, rangeEnd));
            }
        }
        return ranges;
    }

    private boolean isRemovableMessage(ChatMessage message) {
        if (message == null) {
            return false;
        }
        if (message instanceof AssistantProfileSwitchMessage
            || message instanceof InstructionAckMessage
            || message instanceof TranscriptHiddenSystemMessage
            || message instanceof RemovedForSpaceSystemMessage
            || message instanceof ToolCallSummaryMessage
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

    private static class ActiveTurnRange {
        private final int startIndex;
        private final int endIndex;

        private ActiveTurnRange(int startIndex, int endIndex) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
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
