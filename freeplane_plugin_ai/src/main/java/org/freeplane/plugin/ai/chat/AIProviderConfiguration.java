package org.freeplane.plugin.ai.chat;

public final class AIProviderConfiguration {
    private final String providerName;
    private final String serviceAddress;
    private final String modelName;
    private final String openRouterKey;

    public AIProviderConfiguration(String providerName, String serviceAddress, String modelName, String openRouterKey) {
        this.providerName = providerName;
        this.serviceAddress = serviceAddress;
        this.modelName = modelName;
        this.openRouterKey = openRouterKey;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getServiceAddress() {
        return serviceAddress;
    }

    public String getModelName() {
        return modelName;
    }

    public String getOpenRouterKey() {
        return openRouterKey;
    }
}
