package org.freeplane.plugin.ai.chat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.freeplane.plugin.ai.tools.AIToolSet;

public class AIChatService {

    private final AIAssistant assistant;

    public AIChatService(ChatModel chatLanguageModel, AIToolSet toolSet) {
        this.assistant = AiServices.builder(AIAssistant.class)
            .chatModel(chatLanguageModel)
            .systemMessageProvider(toolSet::systemMessageForChat)
            .tools(toolSet)
            .build();
    }

    public String chat(String message) {
        return assistant.chat(message);
    }

    public interface AIAssistant {
        String chat(String message);
    }
}
