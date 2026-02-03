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

public class AssistantProfileChatMemory implements ChatMemory {

    private final Object id;
    private final Function<Object, Integer> maxMessagesProvider;
    private GeneralSystemMessage generalSystemMessage;
    private final List<AssistantProfileSystemMessage> assistantProfileMessages = new ArrayList<>();
    private TranscriptHiddenSystemMessage transcriptHiddenMessage;
    private RemovedForSpaceSystemMessage removedForSpaceMessage;
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
            transcriptHiddenMessage = (TranscriptHiddenSystemMessage) message;
            return;
        }
        if (message instanceof RemovedForSpaceSystemMessage) {
            removedForSpaceMessage = (RemovedForSpaceSystemMessage) message;
            return;
        }
        if (message instanceof AssistantProfileSystemMessage) {
            assistantProfileMessages.add((AssistantProfileSystemMessage) message);
            ensureCapacity();
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
        assistantProfileMessages.clear();
        transcriptHiddenMessage = null;
        removedForSpaceMessage = null;
        conversationMessages.clear();
    }

    private void ensureCapacity() {
        int maxMessages = maxMessagesProvider.apply(id);
        ensureGreaterThanZero(maxMessages, "maxMessages");
        boolean evictedConversation = false;
        while (countedSize() > maxMessages) {
            if (!conversationMessages.isEmpty()) {
                ChatMessage evicted = conversationMessages.remove(0);
                evictedConversation = true;
                if (evicted instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                    while (!conversationMessages.isEmpty()
                        && conversationMessages.get(0) instanceof ToolExecutionResultMessage) {
                        conversationMessages.remove(0);
                    }
                }
            } else if (!assistantProfileMessages.isEmpty()) {
                assistantProfileMessages.remove(0);
            } else {
                break;
            }
        }
        if (evictedConversation) {
            ensureRemovedForSpaceMessage();
        }
        if (evictedConversation && conversationMessages.isEmpty() && !assistantProfileMessages.isEmpty()) {
            assistantProfileMessages.clear();
        }
    }

    private int countedSize() {
        int count = conversationMessages.size() + assistantProfileMessages.size();
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
        messages.addAll(assistantProfileMessages);
        if (transcriptHiddenMessage != null) {
            messages.add(transcriptHiddenMessage);
        }
        if (removedForSpaceMessage != null) {
            messages.add(removedForSpaceMessage);
        }
        messages.addAll(conversationMessages);
        return messages;
    }

    private void ensureRemovedForSpaceMessage() {
        if (removedForSpaceMessage == null) {
            removedForSpaceMessage = new RemovedForSpaceSystemMessage();
        }
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
