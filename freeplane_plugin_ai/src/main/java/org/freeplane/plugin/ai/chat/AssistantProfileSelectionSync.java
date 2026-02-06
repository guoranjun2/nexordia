package org.freeplane.plugin.ai.chat;

import java.util.List;
import java.util.function.Consumer;
import dev.langchain4j.memory.ChatMemory;
import org.freeplane.plugin.ai.chat.history.AssistantProfileTranscriptEntry;
import org.freeplane.plugin.ai.chat.history.ChatTranscriptEntry;
import org.freeplane.plugin.ai.tools.MessageBuilder;

class AssistantProfileSelectionSync {
    private final AssistantProfileSelectionModel selectionModel;
    private final LiveChatController liveChatController;
    private ChatSessionMemoryController chatSessionMemoryController;
    private Consumer<String> profileMessageConsumer;
    private AssistantProfile pendingProfile;
    private String pendingProfileKey;
    private String lastInjectedProfileKey;

    AssistantProfileSelectionSync(AssistantProfileSelectionModel selectionModel, LiveChatController liveChatController) {
        this.selectionModel = selectionModel;
        this.liveChatController = liveChatController;
    }

    void setChatSessionMemoryController(ChatSessionMemoryController chatSessionMemoryController) {
        this.chatSessionMemoryController = chatSessionMemoryController;
    }

    void setProfileMessageConsumer(Consumer<String> profileMessageConsumer) {
        this.profileMessageConsumer = profileMessageConsumer;
    }

    void applyAssistantProfileSelection(AssistantProfile profile) {
        if (profile == null) {
            return;
        }
        AssistantProfileSystemMessage message = new AssistantProfileSystemMessage(
            profile.getName(),
            profile.getPrompt(),
            false);
        String prompt = message.text();
        if (prompt == null || prompt.trim().isEmpty()) {
            return;
        }
        ChatMemory memory = chatSessionMemoryController == null ? null : chatSessionMemoryController.getChatMemory();
        if (memory != null) {
            memory.add(message);
        }
        liveChatController.recordAssistantProfileMessage(message);
        if (profileMessageConsumer != null) {
            profileMessageConsumer.accept(profile.getName());
        }
        lastInjectedProfileKey = profileKey(profile);
    }

    void handleUserSelection(AssistantProfile profile) {
        if (profile == null) {
            return;
        }
        selectionModel.setSelectedProfile(profile, true);
        pendingProfile = profile;
        pendingProfileKey = profileKey(profile);
    }

    AssistantProfile selectFromTranscript() {
        List<ChatTranscriptEntry> entries = liveChatController.snapshotTranscriptEntries();
        if (entries == null || entries.isEmpty()) {
            AssistantProfile selected = selectionModel.getSelectedProfile();
            lastInjectedProfileKey = null;
            pendingProfile = selected;
            pendingProfileKey = profileKey(selected);
            return selected;
        }
        String prompt = findLastAssistantProfilePrompt(entries);
        if (prompt == null || prompt.trim().isEmpty()) {
            lastInjectedProfileKey = null;
            pendingProfile = AssistantProfile.defaultProfile();
            pendingProfileKey = profileKey(pendingProfile);
            return AssistantProfile.defaultProfile();
        }
        AssistantProfile selected = selectionModel.selectByPrompt(prompt);
        lastInjectedProfileKey = profileKey(selected);
        pendingProfile = selected;
        pendingProfileKey = lastInjectedProfileKey;
        return selected;
    }

    void maybeInjectBeforeUserMessage() {
        if (pendingProfile == null || pendingProfileKey == null || pendingProfileKey.trim().isEmpty()) {
            return;
        }
        if (pendingProfileKey.equals(lastInjectedProfileKey)) {
            pendingProfile = null;
            pendingProfileKey = null;
            return;
        }
        applyAssistantProfileSelection(pendingProfile);
        pendingProfile = null;
        pendingProfileKey = null;
    }

    private String findLastAssistantProfilePrompt(List<ChatTranscriptEntry> entries) {
        for (int index = entries.size() - 1; index >= 0; index--) {
            ChatTranscriptEntry entry = entries.get(index);
            if (entry instanceof AssistantProfileTranscriptEntry) {
                AssistantProfileTranscriptEntry profileEntry = (AssistantProfileTranscriptEntry) entry;
                return MessageBuilder.buildAssistantProfileInstruction(
                    profileEntry.getProfileName(),
                    profileEntry.getProfileDefinition(),
                    profileEntry.isHistoricalMarker());
            }
        }
        return null;
    }

    private String profileKey(AssistantProfile profile) {
        if (profile == null) {
            return "";
        }
        if (profile.isCustom()) {
            return "prompt:" + normalize(profile.getPrompt());
        }
        String id = profile.getId();
        if (id == null || id.trim().isEmpty()) {
            return "prompt:" + normalize(profile.getPrompt());
        }
        return "id:" + id.trim();
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim();
    }
}
