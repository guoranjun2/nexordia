package org.freeplane.plugin.ai.chat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class AIChatModelFactory {

    public static final String PROVIDER_NAME_OPENROUTER = "openrouter";
    public static final String PROVIDER_NAME_GEMINI = "gemini";
    public static final String PROVIDER_NAME_OLLAMA = "ollama";
    public static final String DEFAULT_OPENROUTER_SERVICE_ADDRESS = "https://openrouter.ai/api/v1";
    public static final String DEFAULT_GEMINI_SERVICE_ADDRESS = "https://generativelanguage.googleapis.com";
    public static final String DEFAULT_OLLAMA_SERVICE_ADDRESS = "http://localhost:11434";

    private AIChatModelFactory() {
    }

    public static ChatModel createChatLanguageModel(AIProviderConfiguration configuration) {
        AIModelSelection selection = AIModelSelection.fromSelectionValue(configuration.getSelectedModelValue());
        if (selection == null) {
            throw new IllegalArgumentException("Missing model selection");
        }
        String providerName = selection.getProviderName();
        String modelName = selection.getModelName();
        if (PROVIDER_NAME_OPENROUTER.equalsIgnoreCase(providerName)) {
            return OpenAiChatModel.builder()
                .baseUrl(getOpenrouterServiceAddress(configuration))
                .apiKey(configuration.getOpenRouterKey())
                .modelName(modelName)
                .build();
        }
        if (PROVIDER_NAME_GEMINI.equalsIgnoreCase(providerName)) {
            return GoogleAiGeminiChatModel.builder()
                .baseUrl(getGeminiServiceAddress(configuration))
                .apiKey(configuration.getGeminiKey())
                .modelName(modelName)
                .build();
        }
        if (PROVIDER_NAME_OLLAMA.equalsIgnoreCase(providerName)) {
            return OllamaChatModel.builder()
                .baseUrl(getOllamaServiceAddress(configuration))
                .modelName(modelName)
                .build();
        }
        throw new IllegalArgumentException("Unknown provider name: " + providerName);
    }

    private static String getOpenrouterServiceAddress(AIProviderConfiguration configuration) {
        String serviceAddress = configuration.getOpenrouterServiceAddress();
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            return DEFAULT_OPENROUTER_SERVICE_ADDRESS;
        }
        return serviceAddress;
    }

    private static String getGeminiServiceAddress(AIProviderConfiguration configuration) {
        String serviceAddress = configuration.getGeminiServiceAddress();
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            return DEFAULT_GEMINI_SERVICE_ADDRESS;
        }
        return serviceAddress;
    }

    private static String getOllamaServiceAddress(AIProviderConfiguration configuration) {
        String serviceAddress = configuration.getOllamaServiceAddress();
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            return DEFAULT_OLLAMA_SERVICE_ADDRESS;
        }
        return serviceAddress;
    }
}
