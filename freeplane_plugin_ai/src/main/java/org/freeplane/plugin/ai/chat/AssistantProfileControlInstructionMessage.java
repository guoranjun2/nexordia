package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.UserMessage;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class AssistantProfileControlInstructionMessage extends UserMessage {
    private final String profileId;
    private final String profileName;
    private final String profileDefinition;
    private final boolean containsProfileDefinition;

    public AssistantProfileControlInstructionMessage(String profileId,
                                                     String profileName,
                                                     String profileDefinition,
                                                     boolean containsProfileDefinition) {
        super(buildInstructionText(profileName, profileDefinition, containsProfileDefinition));
        this.profileId = profileId == null ? "" : profileId.trim();
        this.profileName = profileName == null ? "" : profileName.trim();
        this.profileDefinition = profileDefinition == null ? "" : profileDefinition.trim();
        this.containsProfileDefinition = containsProfileDefinition && !this.profileDefinition.isEmpty();
    }

    public String getProfileId() {
        return profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public String getProfileDefinition() {
        return profileDefinition;
    }

    public boolean containsProfileDefinition() {
        return containsProfileDefinition;
    }

    public String text() {
        return singleText();
    }

    public AssistantProfileControlInstructionMessage withoutProfileDefinition() {
        if (!containsProfileDefinition) {
            return this;
        }
        return new AssistantProfileControlInstructionMessage(profileId, profileName, profileDefinition, false);
    }

    private static String buildInstructionText(String profileName, String profileDefinition, boolean containsProfileDefinition) {
        String normalizedProfileDefinition = profileDefinition == null ? "" : profileDefinition.trim();
        boolean hasDefinition = containsProfileDefinition && !normalizedProfileDefinition.isEmpty();
        return MessageBuilder.buildAssistantProfileInstruction(profileName, normalizedProfileDefinition, hasDefinition);
    }
}
