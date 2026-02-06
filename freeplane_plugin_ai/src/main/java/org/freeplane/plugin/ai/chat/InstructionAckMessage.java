package org.freeplane.plugin.ai.chat;

import dev.langchain4j.data.message.AiMessage;

class InstructionAckMessage extends AiMessage {
    private static final String ACK_TEXT = "ok";

    InstructionAckMessage() {
        super(ACK_TEXT);
    }
}
