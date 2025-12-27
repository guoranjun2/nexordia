package org.freeplane.plugin.ai;

public final class AIConfiguration {

    public static final String OPENROUTER_KEY_ENVIRONMENT_VARIABLE = "FREEPLANE_OPENROUTER_KEY";
    public static final String OPENROUTER_KEY_SYSTEM_PROPERTY = "freeplane.openrouter.key";
    public static final String PROVIDER_SYSTEM_PROPERTY = "freeplane.ai.provider";
    public static final String SERVICE_ADDRESS_SYSTEM_PROPERTY = "freeplane.ai.service.address";
    public static final String MODEL_NAME_SYSTEM_PROPERTY = "freeplane.ai.model.name";

    private AIConfiguration() {
    }

    public static String getOpenRouterKey() {
        String configuredKey = System.getProperty(OPENROUTER_KEY_SYSTEM_PROPERTY);
        if (configuredKey != null && !configuredKey.isEmpty()) {
            return configuredKey;
        }
        return System.getenv(OPENROUTER_KEY_ENVIRONMENT_VARIABLE);
    }

    public static String getProviderName() {
        return System.getProperty(PROVIDER_SYSTEM_PROPERTY);
    }

    public static String getServiceAddress() {
        return System.getProperty(SERVICE_ADDRESS_SYSTEM_PROPERTY);
    }

    public static String getModelName() {
        return System.getProperty(MODEL_NAME_SYSTEM_PROPERTY);
    }
}
