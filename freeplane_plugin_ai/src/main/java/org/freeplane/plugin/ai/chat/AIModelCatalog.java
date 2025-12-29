package org.freeplane.plugin.ai.chat;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AIModelCatalog {
    private static final long OPENROUTER_REFRESH_INTERVAL_MILLISECONDS = 30L * 60L * 1000L;
    private static final Set<String> OPENROUTER_MODEL_ALLOWLIST = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "deepseek/deepseek-r1-0528:free",
        "meta-llama/llama-3.3-70b-instruct:free",
        "z-ai/glm-4.5-air:free",
        "qwen/qwen-2.5-72b-instruct:free",
        "deepseek/deepseek-r1-0528",
        "anthropic/claude-3.5-sonnet",
        "anthropic/claude-3.5-haiku",
        "openai/gpt-5",
        "openai/gpt-5-mini",
        "openai/gpt-5-nano",
        "openai/gpt-4o",
        "openai/gpt-4o-mini",
        "meta-llama/llama-3.3-70b-instruct",
        "google/gemini-2.5-pro",
        "google/gemini-3-flash-preview"
    )));

    private static final List<String> GEMINI_MODELS = Collections.unmodifiableList(Arrays.asList(
        "gemini-2.5-pro",
        "gemini-3-flash-preview"
    ));

    private static final Object openrouterLock = new Object();
    private static long lastOpenrouterRefreshTime;
    private static List<AIModelDescriptor> cachedOpenrouterModels = Collections.emptyList();

    private static final Object ollamaLock = new Object();
    private static long lastOllamaRefreshTime;
    private static List<AIModelDescriptor> cachedOllamaModels = Collections.emptyList();

    private final AIProviderConfiguration configuration;
    private final Gson gsonParser;

    AIModelCatalog(AIProviderConfiguration configuration) {
        this.configuration = configuration;
        this.gsonParser = new Gson();
    }

    List<AIModelDescriptor> getAvailableModels(boolean allowsRefresh) {
        List<AIModelDescriptor> modelDescriptors = new ArrayList<>();
        if (hasOpenrouterKey()) {
            modelDescriptors.addAll(getOpenrouterModels(allowsRefresh));
        }
        if (hasGeminiKey()) {
            modelDescriptors.addAll(getGeminiModels());
        }
        if (configuration.isOllamaEnabled()) {
            modelDescriptors.addAll(getOllamaModels(allowsRefresh));
        }
        return modelDescriptors;
    }

    private boolean hasOpenrouterKey() {
        String openrouterKey = configuration.getOpenRouterKey();
        return openrouterKey != null && !openrouterKey.isEmpty();
    }

    private boolean hasGeminiKey() {
        String geminiKey = configuration.getGeminiKey();
        return geminiKey != null && !geminiKey.isEmpty();
    }

    private List<AIModelDescriptor> getGeminiModels() {
        List<AIModelDescriptor> modelDescriptors = new ArrayList<>();
        for (String modelName : GEMINI_MODELS) {
            modelDescriptors.add(new AIModelDescriptor(
                AIChatModelFactory.PROVIDER_NAME_GEMINI,
                modelName,
                buildDisplayName(AIChatModelFactory.PROVIDER_NAME_GEMINI, modelName, false),
                false
            ));
        }
        return modelDescriptors;
    }

    private List<AIModelDescriptor> getOpenrouterModels(boolean allowsRefresh) {
        if (!allowsRefresh) {
            return cachedOpenrouterModels;
        }
        synchronized (openrouterLock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastOpenrouterRefreshTime < OPENROUTER_REFRESH_INTERVAL_MILLISECONDS) {
                return cachedOpenrouterModels;
            }
            List<AIModelDescriptor> refreshedModels = fetchOpenrouterModels();
            cachedOpenrouterModels = refreshedModels;
            lastOpenrouterRefreshTime = currentTime;
            return cachedOpenrouterModels;
        }
    }

    private List<AIModelDescriptor> getOllamaModels(boolean allowsRefresh) {
        if (!allowsRefresh) {
            return cachedOllamaModels;
        }
        synchronized (ollamaLock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastOllamaRefreshTime < OPENROUTER_REFRESH_INTERVAL_MILLISECONDS) {
                return cachedOllamaModels;
            }
            List<AIModelDescriptor> refreshedModels = fetchOllamaModels();
            cachedOllamaModels = refreshedModels;
            lastOllamaRefreshTime = currentTime;
            return cachedOllamaModels;
        }
    }

    private List<AIModelDescriptor> fetchOpenrouterModels() {
        String serviceAddress = configuration.getOpenrouterServiceAddress();
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            serviceAddress = AIChatModelFactory.DEFAULT_OPENROUTER_SERVICE_ADDRESS;
        }
        String modelsAddress = serviceAddress.endsWith("/") ? serviceAddress + "models" : serviceAddress + "/models";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(modelsAddress).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("HTTP-Referer", "https://github.com/freeplane/freeplane");
            connection.setRequestProperty("X-Title", "Freeplane");
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Collections.emptyList();
            }
            try (InputStream inputStream = connection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                OpenrouterModelsResponse response = gsonParser.fromJson(reader, OpenrouterModelsResponse.class);
                if (response == null || response.models == null) {
                    return Collections.emptyList();
                }
                List<AIModelDescriptor> modelDescriptors = new ArrayList<>();
                for (OpenrouterModelItem modelItem : response.models) {
                    if (modelItem == null || modelItem.modelIdentifier == null) {
                        continue;
                    }
                    if (!OPENROUTER_MODEL_ALLOWLIST.contains(modelItem.modelIdentifier)) {
                        continue;
                    }
                    boolean isFreeModel = isFreePricing(modelItem.pricing);
                    modelDescriptors.add(new AIModelDescriptor(
                        AIChatModelFactory.PROVIDER_NAME_OPENROUTER,
                        modelItem.modelIdentifier,
                        buildDisplayName(AIChatModelFactory.PROVIDER_NAME_OPENROUTER, modelItem.modelIdentifier, isFreeModel),
                        isFreeModel
                    ));
                }
                return modelDescriptors;
            }
        } catch (IOException exception) {
            return Collections.emptyList();
        }
    }

    private List<AIModelDescriptor> fetchOllamaModels() {
        String serviceAddress = configuration.getOllamaServiceAddress();
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            serviceAddress = AIChatModelFactory.DEFAULT_OLLAMA_SERVICE_ADDRESS;
        }
        String modelsAddress = serviceAddress.endsWith("/") ? serviceAddress + "api/tags" : serviceAddress + "/api/tags";
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(modelsAddress).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return Collections.emptyList();
            }
            try (InputStream inputStream = connection.getInputStream();
                 InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                OllamaModelsResponse response = gsonParser.fromJson(reader, OllamaModelsResponse.class);
                if (response == null || response.models == null) {
                    return Collections.emptyList();
                }
                List<AIModelDescriptor> modelDescriptors = new ArrayList<>();
                for (OllamaModelItem modelItem : response.models) {
                    if (modelItem == null || modelItem.modelName == null || modelItem.modelName.isEmpty()) {
                        continue;
                    }
                    modelDescriptors.add(new AIModelDescriptor(
                        AIChatModelFactory.PROVIDER_NAME_OLLAMA,
                        modelItem.modelName,
                        buildDisplayName(AIChatModelFactory.PROVIDER_NAME_OLLAMA, modelItem.modelName, false),
                        false
                    ));
                }
                return modelDescriptors;
            }
        } catch (IOException exception) {
            return Collections.emptyList();
        }
    }

    private String buildDisplayName(String providerName, String modelName, boolean isFreeModel) {
        String providerDisplayName;
        if (AIChatModelFactory.PROVIDER_NAME_OPENROUTER.equals(providerName)) {
            providerDisplayName = "OpenRouter";
        } else if (AIChatModelFactory.PROVIDER_NAME_GEMINI.equals(providerName)) {
            providerDisplayName = "Gemini";
        } else if (AIChatModelFactory.PROVIDER_NAME_OLLAMA.equals(providerName)) {
            providerDisplayName = "Ollama";
        } else {
            providerDisplayName = providerName;
        }
        String displayName = providerDisplayName + ": " + modelName;
        if (isFreeModel) {
            displayName = displayName + " (free)";
        }
        return displayName;
    }

    private boolean isFreePricing(OpenrouterModelPricing pricing) {
        if (pricing == null) {
            return false;
        }
        return isZeroCost(pricing.promptPrice) && isZeroCost(pricing.completionPrice);
    }

    private boolean isZeroCost(String price) {
        if (price == null || price.isEmpty()) {
            return false;
        }
        try {
            return Double.parseDouble(price) == 0.0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static final class OpenrouterModelsResponse {
        @SerializedName("data")
        private List<OpenrouterModelItem> models;
    }

    private static final class OpenrouterModelItem {
        @SerializedName("id")
        private String modelIdentifier;
        @SerializedName("pricing")
        private OpenrouterModelPricing pricing;
    }

    private static final class OpenrouterModelPricing {
        @SerializedName("prompt")
        private String promptPrice;
        @SerializedName("completion")
        private String completionPrice;
    }

    private static final class OllamaModelsResponse {
        @SerializedName("models")
        private List<OllamaModelItem> models;
    }

    private static final class OllamaModelItem {
        @SerializedName("name")
        private String modelName;
    }
}
