package org.freeplane.plugin.ai.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

class AIModelCatalog {
    private static final long OPENROUTER_REFRESH_INTERVAL_MILLISECONDS = 30L * 60L * 1000L;

    private static final Object openrouterLock = new Object();
    private static long lastOpenrouterRefreshTime;
    private static List<AIModelDescriptor> cachedOpenrouterModels = Collections.emptyList();

    private static final Object ollamaLock = new Object();
    private static long lastOllamaRefreshTime;
    private static List<AIModelDescriptor> cachedOllamaModels = Collections.emptyList();

    private static final Object geminiLock = new Object();
    private static long lastGeminiRefreshTime;
    private static List<AIModelDescriptor> cachedGeminiModels = Collections.emptyList();

    private final AIProviderConfiguration configuration;
    private final ObjectMapper objectMapper;

    AIModelCatalog(AIProviderConfiguration configuration) {
        this.configuration = configuration;
        this.objectMapper = new ObjectMapper();
    }

    List<AIModelDescriptor> getAvailableModels(boolean allowsRefresh) {
        List<AIModelDescriptor> modelDescriptors = new ArrayList<>();
        if (hasOpenrouterKey()) {
            List<AIModelDescriptor> openrouterModels = getOpenrouterModels(allowsRefresh);
            modelDescriptors.addAll(filterModelDescriptors(openrouterModels,
                configuration.getOpenrouterModelAllowlistValue()));
        }
        if (hasGeminiKey()) {
            List<AIModelDescriptor> geminiModels = getGeminiModels(allowsRefresh);
            modelDescriptors.addAll(filterModelDescriptors(geminiModels,
                configuration.getGeminiModelAllowlistValue()));
        }
        if (configuration.isOllamaEnabled()) {
            List<AIModelDescriptor> ollamaModels = getOllamaModels(allowsRefresh);
            modelDescriptors.addAll(filterModelDescriptors(ollamaModels,
                configuration.getOllamaModelAllowlistValue()));
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

    private List<AIModelDescriptor> getGeminiModels(boolean allowsRefresh) {
        if (!allowsRefresh) {
            return cachedGeminiModels;
        }
        synchronized (geminiLock) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastGeminiRefreshTime < OPENROUTER_REFRESH_INTERVAL_MILLISECONDS) {
                return cachedGeminiModels;
            }
            List<AIModelDescriptor> refreshedModels = fetchGeminiModels();
            cachedGeminiModels = refreshedModels;
            lastGeminiRefreshTime = currentTime;
            return cachedGeminiModels;
        }
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
                return parseOpenrouterModelsResponse(reader);
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
                return parseOllamaModelsResponse(reader);
            }
        } catch (IOException exception) {
            return Collections.emptyList();
        }
    }

    private List<AIModelDescriptor> fetchGeminiModels() {
        String serviceAddress = configuration.getGeminiServiceAddress();
        if (serviceAddress == null || serviceAddress.isEmpty()) {
            return Collections.emptyList();
        }
        String geminiKey = configuration.getGeminiKey();
        if (geminiKey == null || geminiKey.isEmpty()) {
            return Collections.emptyList();
        }
        List<AIModelDescriptor> modelDescriptors = new ArrayList<>();
        String pageToken = null;
        do {
            String modelsAddress = buildGeminiModelsAddress(serviceAddress, pageToken);
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(modelsAddress).openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(10000);
                connection.setRequestProperty("x-goog-api-key", geminiKey);
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    return Collections.emptyList();
                }
                try (InputStream inputStream = connection.getInputStream();
                     InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    GeminiModelsPage page = readGeminiModelsPage(reader);
                    modelDescriptors.addAll(page.modelDescriptors);
                    pageToken = page.nextPageToken;
                }
            } catch (IOException exception) {
                return Collections.emptyList();
            }
        } while (pageToken != null && !pageToken.isEmpty());
        return modelDescriptors;
    }

    List<AIModelDescriptor> parseOpenrouterModelsResponse(Reader reader) throws IOException {
        OpenrouterModelsResponse response = objectMapper.readValue(reader, OpenrouterModelsResponse.class);
        if (response == null || response.models == null) {
            return Collections.emptyList();
        }
        List<AIModelDescriptor> modelDescriptors = new ArrayList<>();
        for (OpenrouterModelItem modelItem : response.models) {
            if (modelItem == null || modelItem.modelIdentifier == null) {
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

    List<AIModelDescriptor> parseOllamaModelsResponse(Reader reader) throws IOException {
        OllamaModelsResponse response = objectMapper.readValue(reader, OllamaModelsResponse.class);
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

    List<AIModelDescriptor> parseGeminiModelsResponse(Reader reader) throws IOException {
        GeminiModelsPage page = readGeminiModelsPage(reader);
        return page.modelDescriptors;
    }

    List<AIModelDescriptor> filterModelDescriptors(List<AIModelDescriptor> modelDescriptors,
                                                   String allowlistValue) {
        if (modelDescriptors.isEmpty()) {
            return modelDescriptors;
        }
        List<Pattern> allowlistPatterns = parseModelAllowlistPatterns(allowlistValue);
        if (allowlistPatterns.isEmpty()) {
            return modelDescriptors;
        }
        List<AIModelDescriptor> filteredDescriptors = new ArrayList<>();
        for (AIModelDescriptor modelDescriptor : modelDescriptors) {
            if (modelDescriptor == null) {
                continue;
            }
            if (matchesAllowlist(modelDescriptor.getModelName(), allowlistPatterns)) {
                filteredDescriptors.add(modelDescriptor);
            }
        }
        return filteredDescriptors;
    }

    private List<Pattern> parseModelAllowlistPatterns(String allowlistValue) {
        if (allowlistValue == null || allowlistValue.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Pattern> patterns = new ArrayList<>();
        String[] entries = allowlistValue.split("[,\\r\\n]+");
        for (String entry : entries) {
            String trimmedEntry = entry.trim();
            if (trimmedEntry.isEmpty()) {
                continue;
            }
            patterns.add(Pattern.compile(convertWildcardToRegex(trimmedEntry)));
        }
        return patterns;
    }

    private boolean matchesAllowlist(String modelName, List<Pattern> allowlistPatterns) {
        if (modelName == null || modelName.isEmpty()) {
            return false;
        }
        for (Pattern pattern : allowlistPatterns) {
            if (pattern.matcher(modelName).matches()) {
                return true;
            }
        }
        return false;
    }

    private String convertWildcardToRegex(String wildcardPattern) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < wildcardPattern.length(); index++) {
            char character = wildcardPattern.charAt(index);
            if (character == '*') {
                builder.append(".*");
            } else if (character == '?') {
                builder.append('.');
            } else {
                if ("\\.^$|()[]{}+".indexOf(character) >= 0) {
                    builder.append('\\');
                }
                builder.append(character);
            }
        }
        return builder.toString();
    }

    private GeminiModelsPage readGeminiModelsPage(Reader reader) throws IOException {
        GeminiModelsResponse response = objectMapper.readValue(reader, GeminiModelsResponse.class);
        if (response == null || response.models == null) {
            return new GeminiModelsPage(Collections.emptyList(),
                response == null ? null : response.nextPageToken);
        }
        List<AIModelDescriptor> modelDescriptors = new ArrayList<>();
        for (GeminiModelItem modelItem : response.models) {
            if (modelItem == null || modelItem.modelName == null) {
                continue;
            }
            if (!modelItem.supportsTextGeneration()) {
                continue;
            }
            String modelName = stripGeminiModelPrefix(modelItem.modelName);
            modelDescriptors.add(new AIModelDescriptor(
                AIChatModelFactory.PROVIDER_NAME_GEMINI,
                modelName,
                buildDisplayName(AIChatModelFactory.PROVIDER_NAME_GEMINI, modelName, false),
                false
            ));
        }
        return new GeminiModelsPage(modelDescriptors, response.nextPageToken);
    }

    private String stripGeminiModelPrefix(String modelName) {
        if (modelName.startsWith("models/")) {
            return modelName.substring("models/".length());
        }
        return modelName;
    }

    private String buildGeminiModelsAddress(String serviceAddress, String pageToken) {
        String modelsAddress = serviceAddress.endsWith("/") ? serviceAddress + "models" : serviceAddress + "/models";
        if (pageToken == null || pageToken.isEmpty()) {
            return modelsAddress;
        }
        try {
            String encodedPageToken = URLEncoder.encode(pageToken, StandardCharsets.UTF_8.name());
            return modelsAddress + "?pageToken=" + encodedPageToken;
        } catch (UnsupportedEncodingException exception) {
            return modelsAddress + "?pageToken=" + pageToken;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenrouterModelsResponse {
        @JsonProperty("data")
        private List<OpenrouterModelItem> models;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenrouterModelItem {
        @JsonProperty("id")
        private String modelIdentifier;
        @JsonProperty("pricing")
        private OpenrouterModelPricing pricing;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OpenrouterModelPricing {
        @JsonProperty("prompt")
        private String promptPrice;
        @JsonProperty("completion")
        private String completionPrice;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaModelsResponse {
        @JsonProperty("models")
        private List<OllamaModelItem> models;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class OllamaModelItem {
        @JsonProperty("name")
        private String modelName;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiModelsResponse {
        @JsonProperty("models")
        private List<GeminiModelItem> models;
        @JsonProperty("nextPageToken")
        private String nextPageToken;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeminiModelItem {
        @JsonProperty("name")
        private String modelName;
        @JsonProperty("supportedGenerationMethods")
        private List<String> supportedGenerationMethods;

        private boolean supportsTextGeneration() {
            if (supportedGenerationMethods == null) {
                return false;
            }
            return supportedGenerationMethods.contains("generateContent");
        }
    }

    private static class GeminiModelsPage {
        private final List<AIModelDescriptor> modelDescriptors;
        private final String nextPageToken;

        private GeminiModelsPage(List<AIModelDescriptor> modelDescriptors, String nextPageToken) {
            this.modelDescriptors = modelDescriptors;
            this.nextPageToken = nextPageToken;
        }
    }
}
