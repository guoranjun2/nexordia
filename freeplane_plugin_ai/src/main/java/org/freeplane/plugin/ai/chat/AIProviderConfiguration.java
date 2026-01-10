package org.freeplane.plugin.ai.chat;

import org.freeplane.core.resources.ResourceController;

public class AIProviderConfiguration {
    private static final String AI_PROVIDER_NAME_PROPERTY = "ai_provider_name";
    private static final String AI_MODEL_NAME_PROPERTY = "ai_model_name";
    private static final String AI_SELECTED_MODEL_PROPERTY = "ai_selected_model";
    private static final String AI_OPENROUTER_SERVICE_ADDRESS_PROPERTY = "ai_openrouter_service_address";
    private static final String AI_OPENROUTER_KEY_PROPERTY = "ai_openrouter_key";
    private static final String AI_GEMINI_SERVICE_ADDRESS_PROPERTY = "ai_gemini_service_address";
    private static final String AI_GEMINI_KEY_PROPERTY = "ai_gemini_key";
    private static final String AI_OLLAMA_SERVICE_ADDRESS_PROPERTY = "ai_ollama_service_address";
    private static final String AI_USE_OLLAMA_PROPERTY = "ai_use_ollama";

    private final ResourceController resourceController;

    public AIProviderConfiguration() {
        this.resourceController = ResourceController.getResourceController();
    }

    public String getSelectedModelValue() {
        String selectedModelValue = getStoredSelectedModelValue();
        if (selectedModelValue != null) {
            return selectedModelValue;
        }
        String providerName = resourceController.getProperty(AI_PROVIDER_NAME_PROPERTY);
        String modelName = resourceController.getProperty(AI_MODEL_NAME_PROPERTY);
        if (providerName == null || providerName.isEmpty() || modelName == null || modelName.isEmpty()) {
            return null;
        }
        return AIModelSelection.createSelectionValue(providerName, modelName);
    }

    public String getStoredSelectedModelValue() {
        return resourceController.getProperty(AI_SELECTED_MODEL_PROPERTY);
    }

    public void setSelectedModelValue(String selectionValue) {
        resourceController.setProperty(AI_SELECTED_MODEL_PROPERTY, selectionValue);
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

    public boolean isOllamaEnabled() {
        return resourceController.getBooleanProperty(AI_USE_OLLAMA_PROPERTY);
    }
}
