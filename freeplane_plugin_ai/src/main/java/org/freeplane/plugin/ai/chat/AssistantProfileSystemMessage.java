package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.SystemMessage;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class AssistantProfileSystemMessage extends SystemMessage {
    private final String profileId;
    private final String profileName;
    private final String profileDefinition;
    private final boolean containsProfileDefinition;

    public AssistantProfileSystemMessage(String profileId,
                                         String profileName,
                                         String profileDefinition,
                                         boolean containsProfileDefinition) {
        super(MessageBuilder.buildAssistantProfileInstruction(profileName, profileDefinition, containsProfileDefinition));
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

    public AssistantProfileSystemMessage withoutProfileDefinition() {
        if (!containsProfileDefinition) {
            return this;
        }
        return new AssistantProfileSystemMessage(profileId, profileName, profileDefinition, false);
    }
}
