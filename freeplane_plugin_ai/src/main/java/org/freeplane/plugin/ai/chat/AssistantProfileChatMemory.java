package org.freeplane.plugin.ai.chat;

import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.service.memory.ChatMemoryService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class AssistantProfileChatMemory implements ChatMemory {

    private static final TokenCountEstimator DEFAULT_TOKEN_COUNT_ESTIMATOR =
        new OpenAiTokenCountEstimator(OpenAiChatModelName.GPT_4_O_MINI);

    private final Object id;
    private final Function<Object, Integer> maxTokensProvider;
    private final TokenCountEstimator tokenCountEstimator;
    private GeneralSystemMessage generalSystemMessage;
    private final List<ChatMessage> conversationMessages = new ArrayList<>();
    private final List<Integer> conversationTokenCounts = new ArrayList<>();
    private int conversationTokenTotal;
    private int generalSystemTokenCount;
    private final List<Integer> turnEndIndexes = new ArrayList<>();
    private int currentTurnCount;
    private boolean capacityChecksDeferred;

    private AssistantProfileChatMemory(Builder builder) {
        this.id = ensureNotNull(builder.id, "id");
        this.maxTokensProvider = ensureNotNull(builder.maxTokensProvider, "maxTokensProvider");
        this.tokenCountEstimator = ensureNotNull(builder.tokenCountEstimator, "tokenCountEstimator");
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
                ensureCapacityIfEnabled();
                rebuildTurnBoundaries();
            }
            return;
        }
        if (message instanceof RemovedForSpaceSystemMessage) {
            if (!containsInstructionOfType(RemovedForSpaceSystemMessage.class)) {
                addConversationMessage(message);
                addConversationMessage(new InstructionAckMessage());
                ensureCapacityIfEnabled();
                rebuildTurnBoundaries();
            }
            return;
        }
        if (message instanceof AssistantProfileControlInstructionMessage) {
            shortenStoredProfileInstructions();
            addConversationMessage(message);
            addConversationMessage(new InstructionAckMessage());
            ensureCapacityIfEnabled();
            rebuildTurnBoundaries();
            return;
        }
        if (message instanceof InstructionAckMessage) {
            return;
        }
        if (message instanceof SystemMessage) {
            setGeneralSystemMessage(toGeneralSystemMessage((SystemMessage) message));
            ensureCapacityIfEnabled();
            rebuildTurnBoundaries();
            return;
        }
        addConversationMessage(message);
        ensureCapacityIfEnabled();
        rebuildTurnBoundaries();
    }

    @Override
    public List<ChatMessage> messages() {
        return buildMessages(activeConversationEndIndex());
    }

    @Override
    public void clear() {
        generalSystemMessage = null;
        generalSystemTokenCount = 0;
        conversationMessages.clear();
        conversationTokenCounts.clear();
        conversationTokenTotal = 0;
        turnEndIndexes.clear();
        currentTurnCount = 0;
    }

    public boolean canUndo() {
        return currentTurnCount > 0;
    }

    public int conversationMessageCount() {
        return conversationMessages.size();
    }

    public void truncateConversationMessagesTo(int size) {
        int targetSize = Math.max(0, Math.min(size, conversationMessages.size()));
        while (conversationMessages.size() > targetSize) {
            removeConversationMessage(conversationMessages.size() - 1);
        }
        rebuildTurnBoundaries();
    }

    public boolean canRedo() {
        return currentTurnCount < turnEndIndexes.size();
    }

    public String undo() {
        if (!canUndo()) {
            return "";
        }
        int turnIndex = currentTurnCount - 1;
        int from = turnIndex == 0 ? 0 : turnEndIndexes.get(turnIndex - 1);
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

    public void deferCapacityChecks() {
        capacityChecksDeferred = true;
    }

    public void completeDeferredCapacityChecks() {
        capacityChecksDeferred = false;
        ensureCapacity();
        rebuildTurnBoundaries();
    }

    public void cancelDeferredCapacityChecks() {
        capacityChecksDeferred = false;
    }

    public boolean evictOldestTurn() {
        rebuildTurnBoundaries();
        if (turnEndIndexes.isEmpty()) {
            return false;
        }
        int removeUntil = turnEndIndexes.get(0);
        for (int index = 0; index < removeUntil; index++) {
            removeConversationMessage(0);
        }
        ensureRemovedForSpaceMessage();
        ensureCapacityIfEnabled();
        rebuildTurnBoundaries();
        return true;
    }

    public List<ChatTranscriptEntry> activeTranscriptEntries() {
        List<ChatTranscriptEntry> entries = new ArrayList<>();
        int endIndex = activeConversationEndIndex();
        for (int index = 0; index < endIndex; index++) {
            ChatMessage message = conversationMessages.get(index);
            ChatTranscriptEntry entry = toTranscriptEntry(message);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private void ensureCapacity() {
        int maxTokens = maxTokensProvider.apply(id);
        ensureGreaterThanZero(maxTokens, "maxTokens");
        boolean evictedConversation = false;
        while (totalTokenCount() > maxTokens) {
            int indexToEvict = findFirstCountedMessageIndex();
            if (indexToEvict >= 0) {
                ChatMessage evicted = removeConversationMessage(indexToEvict);
                evictedConversation = true;
                if (indexToEvict < conversationMessages.size()
                    && conversationMessages.get(indexToEvict) instanceof InstructionAckMessage) {
                    removeConversationMessage(indexToEvict);
                }
                if (evicted instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                    while (indexToEvict < conversationMessages.size()
                        && conversationMessages.get(indexToEvict) instanceof ToolExecutionResultMessage) {
                        removeConversationMessage(indexToEvict);
                    }
                }
            } else {
                break;
            }
        }
        if (evictedConversation) {
            ensureRemovedForSpaceMessage();
        }
    }

    private int totalTokenCount() {
        return conversationTokenTotal + generalSystemTokenCount;
    }

    private void ensureCapacityIfEnabled() {
        if (!capacityChecksDeferred) {
            ensureCapacity();
        }
    }

    private List<ChatMessage> buildMessages(int conversationEndIndex) {
        List<ChatMessage> messages = new ArrayList<>();
        if (generalSystemMessage != null) {
            messages.add(generalSystemMessage);
        }
        int endIndex = Math.max(0, Math.min(conversationEndIndex, conversationMessages.size()));
        for (int index = 0; index < endIndex; index++) {
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

    private int activeConversationEndIndex() {
        if (canRedo()) {
            return currentTurnCount == 0 ? 0 : turnEndIndexes.get(currentTurnCount - 1);
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

    private void ensureRemovedForSpaceMessage() {
        if (!containsInstructionOfType(RemovedForSpaceSystemMessage.class)) {
            int insertionIndex = findFirstCountedMessageIndex();
            if (insertionIndex < 0) {
                insertionIndex = conversationMessages.size();
            }
            addConversationMessage(insertionIndex, new RemovedForSpaceSystemMessage());
            addConversationMessage(insertionIndex + 1, new InstructionAckMessage());
        }
    }

    private boolean isNonCountedMessage(ChatMessage message) {
        return message instanceof TranscriptHiddenSystemMessage
            || message instanceof RemovedForSpaceSystemMessage
            || message instanceof InstructionAckMessage;
    }

    private int findFirstCountedMessageIndex() {
        for (int index = 0; index < conversationMessages.size(); index++) {
            if (!isNonCountedMessage(conversationMessages.get(index))) {
                return index;
            }
        }
        return -1;
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
            if (message instanceof AiMessage && !(message instanceof InstructionAckMessage)) {
                turnEndIndexes.add(index + 1);
            }
        }
        currentTurnCount = turnEndIndexes.size();
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
        generalSystemTokenCount = message == null ? 0 : estimateTokenCount(message);
    }

    private void addConversationMessage(ChatMessage message) {
        addConversationMessage(conversationMessages.size(), message);
    }

    private void addConversationMessage(int index, ChatMessage message) {
        conversationMessages.add(index, message);
        int tokenCount = estimateTokenCount(message);
        conversationTokenCounts.add(index, tokenCount);
        conversationTokenTotal += tokenCount;
    }

    private ChatMessage removeConversationMessage(int index) {
        conversationTokenTotal -= conversationTokenCounts.remove(index);
        return conversationMessages.remove(index);
    }

    private void replaceConversationMessage(int index, ChatMessage message) {
        conversationMessages.set(index, message);
        int updatedTokenCount = estimateTokenCount(message);
        int previousTokenCount = conversationTokenCounts.set(index, updatedTokenCount);
        conversationTokenTotal += updatedTokenCount - previousTokenCount;
    }

    private int estimateTokenCount(ChatMessage message) {
        return tokenCountEstimator.estimateTokenCountInMessage(message);
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
        private TokenCountEstimator tokenCountEstimator = DEFAULT_TOKEN_COUNT_ESTIMATOR;

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

        public Builder tokenCountEstimator(TokenCountEstimator tokenCountEstimator) {
            this.tokenCountEstimator = tokenCountEstimator;
            return this;
        }

        public AssistantProfileChatMemory build() {
            return new AssistantProfileChatMemory(this);
        }
    }
}
