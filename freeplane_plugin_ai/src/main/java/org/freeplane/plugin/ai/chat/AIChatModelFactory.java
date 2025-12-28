package org.freeplane.plugin.ai.chat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public final class AIChatModelFactory {

    public static final String PROVIDER_NAME_OPENROUTER = "openrouter";
    public static final String PROVIDER_NAME_GEMINI = "gemini";
    public static final String PROVIDER_NAME_OLLAMA = "ollama";
    public static final String DEFAULT_OPENROUTER_SERVICE_ADDRESS = "https://openrouter.ai/api/v1";
    public static final String DEFAULT_GEMINI_SERVICE_ADDRESS = "https://generativelanguage.googleapis.com";
    public static final String DEFAULT_OLLAMA_SERVICE_ADDRESS = "http://localhost:11434";

    private AIChatModelFactory() {
    }

    public static ChatModel createChatLanguageModel(AIProviderConfiguration configuration) {
        String providerName = configuration.getProviderName();
        if (PROVIDER_NAME_OPENROUTER.equals(providerName)) {
            return OpenAiChatModel.builder()
                .baseUrl(getOpenrouterServiceAddress(configuration))
                .apiKey(configuration.getOpenRouterKey())
                .modelName(configuration.getModelName())
                .build();
        }
        if (PROVIDER_NAME_GEMINI.equals(providerName)) {
            return GoogleAiGeminiChatModel.builder()
                .baseUrl(getGeminiServiceAddress(configuration))
                .apiKey(configuration.getGeminiKey())
                .modelName(configuration.getModelName())
                .build();
        }
        if (PROVIDER_NAME_OLLAMA.equals(providerName)) {
            return OllamaChatModel.builder()
                .baseUrl(getOllamaServiceAddress(configuration))
                .modelName(configuration.getModelName())
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
