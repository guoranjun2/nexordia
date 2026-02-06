package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.SystemMessage;
import org.freeplane.plugin.ai.tools.MessageBuilder;

public class AssistantProfileSystemMessage extends SystemMessage {
    private final String profileName;
    private final String profileDefinition;
    private final boolean historicalMarker;

    public AssistantProfileSystemMessage(String profileName, String profileDefinition, boolean historicalMarker) {
        super(MessageBuilder.buildAssistantProfileInstruction(profileName, profileDefinition, historicalMarker));
        this.profileName = profileName == null ? "" : profileName.trim();
        this.profileDefinition = profileDefinition == null ? "" : profileDefinition.trim();
        this.historicalMarker = historicalMarker || this.profileDefinition.isEmpty();
    }

    public String getProfileName() {
        return profileName;
    }

    public String getProfileDefinition() {
        return profileDefinition;
    }

    public boolean isHistoricalMarker() {
        return historicalMarker;
    }

    public AssistantProfileSystemMessage toHistoricalMarker() {
        if (historicalMarker) {
            return this;
        }
        return new AssistantProfileSystemMessage(profileName, profileDefinition, true);
    }
}
