package org.freeplane.plugin.ai.chat;

import java.util.Objects;
import org.freeplane.core.util.TextUtils;

class AIModelDescriptor {
    private final String providerName;
    private final String modelName;
    private final String displayName;
    private final boolean isFreeModel;
    private final boolean unavailable;

    AIModelDescriptor(String providerName, String modelName, String displayName, boolean isFreeModel) {
        this(providerName, modelName, displayName, isFreeModel, false);
    }

    AIModelDescriptor(String providerName, String modelName, String displayName, boolean isFreeModel,
                      boolean unavailable) {
        this.providerName = Objects.requireNonNull(providerName, "providerName");
        this.modelName = Objects.requireNonNull(modelName, "modelName");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.isFreeModel = isFreeModel;
        this.unavailable = unavailable;
    }

    static AIModelDescriptor unavailable(String providerName, String modelName) {
        String availableDisplayName = providerDisplayName(providerName) + ": " + modelName;
        return new AIModelDescriptor(
            providerName,
            modelName,
            TextUtils.format("ai_unavailable_format", availableDisplayName),
            false,
            true);
    }

    String getProviderName() {
        return providerName;
    }

    String getModelName() {
        return modelName;
    }

    String getDisplayName() {
        return displayName;
    }

    boolean isFreeModel() {
        return isFreeModel;
    }

    boolean isUnavailable() {
        return unavailable;
    }

    String getSelectionValue() {
        return AIModelSelection.createSelectionValue(providerName, modelName);
    }

    @Override
    public String toString() {
        return displayName;
    }

    private static String providerDisplayName(String providerName) {
        if (AIChatModelFactory.PROVIDER_NAME_OPENROUTER.equals(providerName)) {
            return "OpenRouter";
        }
        if (AIChatModelFactory.PROVIDER_NAME_GEMINI.equals(providerName)) {
            return "Gemini";
        }
        if (AIChatModelFactory.PROVIDER_NAME_OLLAMA.equals(providerName)) {
            return "Ollama";
        }
        return providerName;
    }
}
