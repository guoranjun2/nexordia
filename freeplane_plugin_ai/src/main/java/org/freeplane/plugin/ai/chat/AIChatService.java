package org.freeplane.plugin.ai.chat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.observability.api.event.AiServiceErrorEvent;
import dev.langchain4j.observability.api.listener.AiServiceListener;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolErrorHandlerResult;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;

import static dev.langchain4j.internal.Utils.isNullOrBlank;

import org.freeplane.plugin.ai.tools.AIToolSet;

public class AIChatService {

    private final AIAssistant assistant;

    public AIChatService(ChatModel chatLanguageModel, AIToolSet toolSet) {
        this.assistant = AiServices.builder(AIAssistant.class)
            .chatModel(chatLanguageModel)
            .systemMessageProvider(toolSet::systemMessageForChat)
            .registerListener(new AiServiceListener<AiServiceErrorEvent>() {

				@Override
				public Class<AiServiceErrorEvent> getEventClass() {
					return AiServiceErrorEvent.class;
				}

				@Override
				public void onEvent(AiServiceErrorEvent event) {
					event.error().printStackTrace();
				}

            })
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
