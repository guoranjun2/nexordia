package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public final class AIProviderConfiguration {
    private static final String AI_PROVIDER_NAME_PROPERTY = "ai_provider_name";
    private static final String AI_MODEL_NAME_PROPERTY = "ai_model_name";
    private static final String AI_OPENROUTER_SERVICE_ADDRESS_PROPERTY = "ai_openrouter_service_address";
    private static final String AI_OPENROUTER_KEY_PROPERTY = "ai_openrouter_key";
    private static final String AI_GEMINI_SERVICE_ADDRESS_PROPERTY = "ai_gemini_service_address";
    private static final String AI_GEMINI_KEY_PROPERTY = "ai_gemini_key";
    private static final String AI_OLLAMA_SERVICE_ADDRESS_PROPERTY = "ai_ollama_service_address";

    private final ResourceController resourceController;

    public AIProviderConfiguration() {
        this.resourceController = ResourceController.getResourceController();
    }

    public String getProviderName() {
        return resourceController.getProperty(AI_PROVIDER_NAME_PROPERTY);
    }

    public String getModelName() {
        return resourceController.getProperty(AI_MODEL_NAME_PROPERTY);
    }

    public String getOpenrouterServiceAddress() {
        return resourceController.getProperty(AI_OPENROUTER_SERVICE_ADDRESS_PROPERTY);
    }

    public String getOpenRouterKey() {
        return resourceController.getProperty(AI_OPENROUTER_KEY_PROPERTY);
    }

    public String getGeminiServiceAddress() {
        return resourceController.getProperty(AI_GEMINI_SERVICE_ADDRESS_PROPERTY);
    }

    public String getGeminiKey() {
        return resourceController.getProperty(AI_GEMINI_KEY_PROPERTY);
    }

    public String getOllamaServiceAddress() {
        return resourceController.getProperty(AI_OLLAMA_SERVICE_ADDRESS_PROPERTY);
    }
}
