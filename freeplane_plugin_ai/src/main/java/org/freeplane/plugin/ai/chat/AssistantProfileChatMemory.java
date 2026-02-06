package org.freeplane.plugin.ai.chat;

import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static dev.langchain4j.internal.ValidationUtils.ensureNotNull;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.service.memory.ChatMemoryService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class AssistantProfileChatMemory implements ChatMemory {

    private final Object id;
    private final Function<Object, Integer> maxMessagesProvider;
    private GeneralSystemMessage generalSystemMessage;
    private final List<ChatMessage> conversationMessages = new ArrayList<>();

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
        ensureCapacity();
        if (message instanceof TranscriptHiddenSystemMessage) {
            if (!containsInstructionOfType(TranscriptHiddenSystemMessage.class)) {
                conversationMessages.add(message);
                conversationMessages.add(new InstructionAckMessage());
                ensureCapacity();
            }
            return;
        }
        if (message instanceof RemovedForSpaceSystemMessage) {
            if (!containsInstructionOfType(RemovedForSpaceSystemMessage.class)) {
                conversationMessages.add(message);
                conversationMessages.add(new InstructionAckMessage());
                ensureCapacity();
            }
            return;
        }
        if (message instanceof AssistantProfileSystemMessage) {
            shortenStoredProfileInstructions();
            conversationMessages.add(message);
            conversationMessages.add(new InstructionAckMessage());
            ensureCapacity();
            return;
        }
        if (message instanceof InstructionAckMessage) {
            return;
        }
        if (message instanceof SystemMessage) {
            generalSystemMessage = toGeneralSystemMessage((SystemMessage) message);
            ensureCapacity();
            return;
        }
        conversationMessages.add(message);
        ensureCapacity();
    }

    @Override
    public List<ChatMessage> messages() {
        ensureCapacity();
        return buildMessages();
    }

    @Override
    public void clear() {
        generalSystemMessage = null;
        conversationMessages.clear();
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

    private List<ChatMessage> buildMessages() {
        List<ChatMessage> messages = new ArrayList<>();
        if (generalSystemMessage != null) {
            messages.add(generalSystemMessage);
        }
        for (ChatMessage message : conversationMessages) {
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
