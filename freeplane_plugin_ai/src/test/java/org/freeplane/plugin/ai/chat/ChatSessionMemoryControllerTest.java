package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
import org.junit.Test;

public class ChatSessionMemoryControllerTest {
    @Test
    public void getChatMemory_returnsNullWhenDisabled() {
        ChatMemorySettings settings = new ChatMemorySettings(ChatMemoryMode.DISABLED, 5);
        ChatSessionMemoryController uut = new ChatSessionMemoryController(settings);

        ChatMemory chatMemory = uut.getChatMemory();

        assertThat(chatMemory).isNull();
    }

    @Test
    public void clearChatMemory_removesMessages() {
        ChatMemorySettings settings = new ChatMemorySettings(ChatMemoryMode.MESSAGE_WINDOW, 5);
        ChatSessionMemoryController uut = new ChatSessionMemoryController(settings);
        ChatMemory chatMemory = uut.getChatMemory();
        chatMemory.add(UserMessage.from("hello"));

        uut.clearChatMemory();

        assertThat(chatMemory.messages()).isEmpty();
    }
}
