package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptRole;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class ChatSessionMemoryController {
    private final ChatMemorySettings chatMemorySettings;
    private ChatMemory chatMemory;

    public ChatSessionMemoryController() {
        this(new ChatMemorySettings());
    }

    ChatSessionMemoryController(ChatMemorySettings chatMemorySettings) {
        this.chatMemorySettings = Objects.requireNonNull(chatMemorySettings, "chatMemorySettings");
    }

    public ChatMemory getChatMemory() {
        if (chatMemorySettings.getChatMemoryMode() == ChatMemoryMode.DISABLED) {
            return null;
        }
        if (chatMemory == null) {
            chatMemory = AssistantProfileChatMemory.withMaxMessages(chatMemorySettings.getMaximumMessageCount());
        }
        return chatMemory;
    }

    public void clearChatMemory() {
        if (chatMemory != null) {
            chatMemory.clear();
        }
    }

    public List<ChatMessage> snapshotMessages() {
        ChatMemory memory = getChatMemory();
        if (memory == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(memory.messages());
    }

    public void restoreMessages(List<ChatMessage> messages) {
        ChatMemory memory = getChatMemory();
        if (memory == null) {
            return;
        }
        memory.clear();
        if (messages == null) {
            return;
        }
        for (ChatMessage message : messages) {
            if (message != null) {
                memory.add(message);
            }
        }
    }

    public void seedTranscript(Iterable<ChatTranscriptEntry> entries, String systemGuard) {
        ChatMemory memory = getChatMemory();
        if (memory == null) {
            return;
        }
        memory.clear();
        if (systemGuard != null && !systemGuard.trim().isEmpty()) {
            memory.add(new GeneralSystemMessage(systemGuard));
        }
        if (entries == null) {
            return;
        }
        for (ChatTranscriptEntry entry : entries) {
            ChatMessage message = toChatMessage(entry);
            if (message != null) {
                memory.add(message);
            }
        }
    }

    public void seedTranscriptWithHiddenExchange(Iterable<ChatTranscriptEntry> entries,
                                                 String hiddenSystemMessage) {
        ChatMemory memory = getChatMemory();
        if (memory == null) {
            return;
        }
        memory.clear();
        if (entries == null) {
            return;
        }
        for (ChatTranscriptEntry entry : entries) {
            ChatMessage message = toChatMessage(entry);
            if (message != null) {
                memory.add(message);
            }
        }
        if (hiddenSystemMessage != null && !hiddenSystemMessage.trim().isEmpty()) {
            memory.add(new TranscriptHiddenSystemMessage(hiddenSystemMessage));
        }
    }

    private ChatMessage toChatMessage(ChatTranscriptEntry entry) {
        if (entry == null || entry.getRole() == null) {
            return null;
        }
        if (entry.getRole() == ChatTranscriptRole.ASSISTANT) {
            if (entry.getText() == null) {
                return null;
            }
            return new AiMessage(entry.getText());
        }
        if (entry.getRole() == ChatTranscriptRole.ASSISTANT_PROFILE_SYSTEM) {
            String instructionText = buildAssistantProfileInstructionText(entry);
            if (instructionText == null || instructionText.trim().isEmpty()) {
                return null;
            }
            return MessageBuilder.buildSystemInstructionUserMessage(instructionText);
        }
        if (entry.getRole() == ChatTranscriptRole.REMOVED_FOR_SPACE_SYSTEM) {
            if (entry.getText() == null) {
                return null;
            }
            return MessageBuilder.buildSystemInstructionUserMessage(entry.getText());
        }
        if (entry.getText() == null) {
            return null;
        }
        return new UserMessage(entry.getText());
    }

    private String buildAssistantProfileInstructionText(ChatTranscriptEntry entry) {
        if (entry instanceof AssistantProfileTranscriptEntry) {
            AssistantProfileTranscriptEntry assistantProfileEntry = (AssistantProfileTranscriptEntry) entry;
            return MessageBuilder.buildAssistantProfileInstruction(
                assistantProfileEntry.getProfileName(),
                "",
                assistantProfileEntry.containsProfileDefinition());
        }
        return null;
    }
}
