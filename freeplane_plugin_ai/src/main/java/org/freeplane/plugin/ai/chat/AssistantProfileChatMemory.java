package org.freeplane.plugin.ai.chat;

import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.memory.ChatMemoryService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class AssistantProfileChatMemory implements ChatMemory {

    private final Object id;
    private final Function<Object, Integer> maxMessagesProvider;
    private GeneralSystemMessage generalSystemMessage;
    private final List<ChatMessage> conversationMessages = new ArrayList<>();
    private final List<Integer> turnEndIndexes = new ArrayList<>();
    private int currentTurnCount;

    private AssistantProfileChatMemory(Builder builder) {
        this.id = ensureNotNull(builder.id, "id");
        this.maxMessagesProvider = ensureNotNull(builder.maxMessagesProvider, "maxMessagesProvider");
        ensureGreaterThanZero(this.maxMessagesProvider.apply(this.id), "maxMessages");
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
                conversationMessages.add(message);
                conversationMessages.add(new InstructionAckMessage());
                ensureCapacity();
                rebuildTurnBoundaries();
            }
            return;
        }
        if (message instanceof RemovedForSpaceSystemMessage) {
            if (!containsInstructionOfType(RemovedForSpaceSystemMessage.class)) {
                conversationMessages.add(message);
                conversationMessages.add(new InstructionAckMessage());
                ensureCapacity();
                rebuildTurnBoundaries();
            }
            return;
        }
        if (message instanceof AssistantProfileSystemMessage) {
            shortenStoredProfileInstructions();
            conversationMessages.add(message);
            conversationMessages.add(new InstructionAckMessage());
            ensureCapacity();
            rebuildTurnBoundaries();
            return;
        }
        if (message instanceof InstructionAckMessage) {
            return;
        }
        if (message instanceof SystemMessage) {
            generalSystemMessage = toGeneralSystemMessage((SystemMessage) message);
            ensureCapacity();
            rebuildTurnBoundaries();
            return;
        }
        conversationMessages.add(message);
        ensureCapacity();
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
        turnEndIndexes.clear();
        currentTurnCount = 0;
    }

    public boolean canUndo() {
        return currentTurnCount > 0;
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
        int maxMessages = maxMessagesProvider.apply(id);
        ensureGreaterThanZero(maxMessages, "maxMessages");
        boolean evictedConversation = false;
        while (countedSize() > maxMessages) {
            int indexToEvict = findFirstCountedMessageIndex();
            if (indexToEvict >= 0) {
                ChatMessage evicted = conversationMessages.remove(indexToEvict);
                evictedConversation = true;
                if (indexToEvict < conversationMessages.size()
                    && conversationMessages.get(indexToEvict) instanceof InstructionAckMessage) {
                    conversationMessages.remove(indexToEvict);
                }
                if (evicted instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                    while (indexToEvict < conversationMessages.size()
                        && conversationMessages.get(indexToEvict) instanceof ToolExecutionResultMessage) {
                        conversationMessages.remove(indexToEvict);
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

    private int countedSize() {
        int count = 0;
        for (ChatMessage conversationMessage : conversationMessages) {
            if (!isNonCountedMessage(conversationMessage)) {
                count++;
            }
        }
        if (generalSystemMessage != null) {
            count += 1;
        }
        return count;
    }

    private List<ChatMessage> buildMessages(int conversationEndIndex) {
        List<ChatMessage> messages = new ArrayList<>();
        if (generalSystemMessage != null) {
            messages.add(generalSystemMessage);
        }
        int endIndex = Math.max(0, Math.min(conversationEndIndex, conversationMessages.size()));
        for (int index = 0; index < endIndex; index++) {
            ChatMessage message = conversationMessages.get(index);
            if (message instanceof AssistantProfileSystemMessage
                || message instanceof TranscriptHiddenSystemMessage
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
            if (message instanceof AssistantProfileSystemMessage) {
                AssistantProfileSystemMessage profileMessage = (AssistantProfileSystemMessage) message;
                conversationMessages.set(index, profileMessage.withoutProfileDefinition());
            }
        }
    }

    private void ensureRemovedForSpaceMessage() {
        if (!containsInstructionOfType(RemovedForSpaceSystemMessage.class)) {
            int insertionIndex = findFirstCountedMessageIndex();
            if (insertionIndex < 0) {
                insertionIndex = conversationMessages.size();
            }
            conversationMessages.add(insertionIndex, new RemovedForSpaceSystemMessage());
            conversationMessages.add(insertionIndex + 1, new InstructionAckMessage());
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
            conversationMessages.remove(conversationMessages.size() - 1);
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
        if (message instanceof AssistantProfileSystemMessage) {
            AssistantProfileSystemMessage profileMessage = (AssistantProfileSystemMessage) message;
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

    public static Builder builder() {
        return new Builder();
    }

    public static AssistantProfileChatMemory withMaxMessages(int maxMessages) {
        return builder().maxMessages(maxMessages).build();
    }

    public static class Builder {

        private Object id = ChatMemoryService.DEFAULT;
        private Function<Object, Integer> maxMessagesProvider;

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder maxMessages(Integer maxMessages) {
            this.maxMessagesProvider = ignored -> maxMessages;
            return this;
        }

        public Builder dynamicMaxMessages(Function<Object, Integer> maxMessagesProvider) {
            this.maxMessagesProvider = maxMessagesProvider;
            return this;
        }

        public AssistantProfileChatMemory build() {
            return new AssistantProfileChatMemory(this);
        }
    }
}
