package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.SystemMessage;

public class AssistantProfileSystemMessage extends SystemMessage {
    public AssistantProfileSystemMessage(String text) {
        super(text);
    }
}
