package org.freeplane.plugin.ai.chat;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public final class AIChatModelFactory {

    public static final String PROVIDER_NAME_OPENROUTER = "openrouter";
    public static final String PROVIDER_NAME_OLLAMA = "ollama";
    public static final String DEFAULT_OPENROUTER_SERVICE_ADDRESS = "https://openrouter.ai/api/v1";
    public static final String DEFAULT_OLLAMA_SERVICE_ADDRESS = "http://localhost:11434";

    private AIChatModelFactory() {
    }

    public static ChatModel createChatLanguageModel(AIProviderConfiguration configuration) {
        String providerName = configuration.getProviderName();
        if (PROVIDER_NAME_OPENROUTER.equals(providerName)) {
            return OpenAiChatModel.builder()
                .baseUrl(getServiceAddress(configuration, DEFAULT_OPENROUTER_SERVICE_ADDRESS))
                .apiKey(configuration.getOpenRouterKey())
                .modelName(configuration.getModelName())
                .build();
        }
        if (PROVIDER_NAME_OLLAMA.equals(providerName)) {
            return OllamaChatModel.builder()
                .baseUrl(getServiceAddress(configuration, DEFAULT_OLLAMA_SERVICE_ADDRESS))
                .modelName(configuration.getModelName())
                .build();
        }
        throw new IllegalArgumentException("Unknown provider name: " + providerName);
    }

    private static String getServiceAddress(AIProviderConfiguration configuration, String defaultAddress) {
        String serviceAddress = configuration.getServiceAddress();
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            return defaultAddress;
        }
        return serviceAddress;
    }
}
