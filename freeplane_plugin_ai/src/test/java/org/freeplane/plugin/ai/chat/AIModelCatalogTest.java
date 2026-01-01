package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.StringReader;
import java.util.List;

import org.junit.Test;

public class AIModelCatalogTest {
    @Test
    public void parseOpenrouterModelsResponse_returnsAllowedModels() throws Exception {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        AIModelCatalog uut = new AIModelCatalog(configuration);
        String responsePayload = "{\"data\":["
            + "{\"id\":\"openai/gpt-5\",\"pricing\":{\"prompt\":\"0\",\"completion\":\"0\"}},"
            + "{\"id\":\"unknown/model\",\"pricing\":{\"prompt\":\"0.01\",\"completion\":\"0.02\"}},"
            + "{\"id\":null},"
            + "null"
            + "]}";

        List<AIModelDescriptor> modelDescriptors = uut.parseOpenrouterModelsResponse(new StringReader(responsePayload));

        assertThat(modelDescriptors).hasSize(1);
        AIModelDescriptor descriptor = modelDescriptors.get(0);
        assertThat(descriptor.getProviderName()).isEqualTo(AIChatModelFactory.PROVIDER_NAME_OPENROUTER);
        assertThat(descriptor.getModelName()).isEqualTo("openai/gpt-5");
        assertThat(descriptor.isFreeModel()).isTrue();
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
}
