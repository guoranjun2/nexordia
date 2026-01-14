package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class AIModelCatalogTest {
    @Test
    public void parseOpenrouterModelsResponse_returnsModels() throws Exception {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        AIModelCatalog uut = new AIModelCatalog(configuration);
        String responsePayload = "{\"data\":["
            + "{\"id\":\"openai/gpt-5\",\"pricing\":{\"prompt\":\"0\",\"completion\":\"0\"}},"
            + "{\"id\":\"unknown/model\",\"pricing\":{\"prompt\":\"0.01\",\"completion\":\"0.02\"}},"
            + "{\"id\":null},"
            + "null"
            + "]}";

        List<AIModelDescriptor> modelDescriptors = uut.parseOpenrouterModelsResponse(new StringReader(responsePayload));

        assertThat(modelDescriptors).hasSize(2);
        AIModelDescriptor firstDescriptor = modelDescriptors.get(0);
        assertThat(firstDescriptor.getProviderName()).isEqualTo(AIChatModelFactory.PROVIDER_NAME_OPENROUTER);
        assertThat(firstDescriptor.getModelName()).isEqualTo("openai/gpt-5");
        assertThat(firstDescriptor.isFreeModel()).isTrue();
        AIModelDescriptor secondDescriptor = modelDescriptors.get(1);
        assertThat(secondDescriptor.getModelName()).isEqualTo("unknown/model");
        assertThat(secondDescriptor.isFreeModel()).isFalse();
    }

    @Test
    public void parseOllamaModelsResponse_returnsModelNames() throws Exception {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        AIModelCatalog uut = new AIModelCatalog(configuration);
        String responsePayload = "{\"models\":["
            + "{\"name\":\"llama3\"},"
            + "{\"name\":\"mistral\"},"
            + "{\"name\":\"\"},"
            + "{}"
            + "]}";

        List<AIModelDescriptor> modelDescriptors = uut.parseOllamaModelsResponse(new StringReader(responsePayload));

        assertThat(modelDescriptors).extracting(AIModelDescriptor::getModelName)
            .containsExactly("llama3", "mistral");
    }

    @Test
    public void parseGeminiModelsResponse_returnsTextModels() throws Exception {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        AIModelCatalog uut = new AIModelCatalog(configuration);
        String responsePayload = "{\"models\":["
            + "{\"name\":\"models/gemini-2.5-pro\",\"supportedGenerationMethods\":[\"generateContent\"]},"
            + "{\"name\":\"models/gemini-vision\",\"supportedGenerationMethods\":[\"countTokens\"]},"
            + "{\"name\":null},"
            + "null"
            + "]}";

        List<AIModelDescriptor> modelDescriptors = uut.parseGeminiModelsResponse(new StringReader(responsePayload));

        assertThat(modelDescriptors).hasSize(1);
        AIModelDescriptor descriptor = modelDescriptors.get(0);
        assertThat(descriptor.getProviderName()).isEqualTo(AIChatModelFactory.PROVIDER_NAME_GEMINI);
        assertThat(descriptor.getModelName()).isEqualTo("gemini-2.5-pro");
    }

    @Test
    public void filterModelDescriptors_appliesWildcardAllowlist() {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        AIModelCatalog uut = new AIModelCatalog(configuration);
        List<AIModelDescriptor> modelDescriptors = Arrays.asList(
            new AIModelDescriptor("openrouter", "openai/gpt-5", "OpenRouter: openai/gpt-5", false),
            new AIModelDescriptor("gemini", "gemini-3-flash-preview", "Gemini: gemini-3-flash-preview", false),
            new AIModelDescriptor("ollama", "llama3", "Ollama: llama3", false)
        );

        List<AIModelDescriptor> filteredDescriptors = uut.filterModelDescriptors(
            modelDescriptors,
            "openai/*, *-preview"
        );

        assertThat(filteredDescriptors).extracting(AIModelDescriptor::getModelName)
            .containsExactly("openai/gpt-5", "gemini-3-flash-preview");
    }
}
