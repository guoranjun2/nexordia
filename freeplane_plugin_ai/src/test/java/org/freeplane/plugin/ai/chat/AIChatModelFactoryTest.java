package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.ChatModel;
import java.lang.reflect.Field;
import org.junit.Test;

public class AIChatModelFactoryTest {

    @Test
    public void createChatLanguageModel_setsMaxRetriesForOpenRouter() throws Exception {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        when(configuration.getSelectedModelValue())
            .thenReturn(AIModelSelection.createSelectionValue(AIChatModelFactory.PROVIDER_NAME_OPENROUTER, "openai/gpt-5"));
        when(configuration.getOpenRouterKey()).thenReturn("test-key");
        when(configuration.getOpenrouterServiceAddress()).thenReturn("https://openrouter.ai/api/v1");

        ChatModel chatModel = AIChatModelFactory.createChatLanguageModel(configuration);

        assertThat(fieldValue(chatModel, "maxRetries")).isEqualTo(AIChatModelFactory.CHAT_MODEL_MAX_RETRIES);
    }

    @Test
    public void createChatLanguageModel_setsMaxRetriesForGemini() throws Exception {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        when(configuration.getSelectedModelValue())
            .thenReturn(AIModelSelection.createSelectionValue(AIChatModelFactory.PROVIDER_NAME_GEMINI, "gemini-2.0-flash"));
        when(configuration.getGeminiKey()).thenReturn("test-key");
        when(configuration.getGeminiServiceAddress()).thenReturn("https://generativelanguage.googleapis.com/v1beta");

        ChatModel chatModel = AIChatModelFactory.createChatLanguageModel(configuration);

        assertThat(fieldValue(chatModel, "maximumRetries")).isEqualTo(AIChatModelFactory.CHAT_MODEL_MAX_RETRIES);
    }

    @Test
    public void createChatLanguageModel_setsMaxRetriesForOllama() throws Exception {
        AIProviderConfiguration configuration = mock(AIProviderConfiguration.class);
        when(configuration.getSelectedModelValue())
            .thenReturn(AIModelSelection.createSelectionValue(AIChatModelFactory.PROVIDER_NAME_OLLAMA, "llama3.2"));
        when(configuration.getOllamaServiceAddress()).thenReturn("http://localhost:11434");

        ChatModel chatModel = AIChatModelFactory.createChatLanguageModel(configuration);

        assertThat(fieldValue(chatModel, "maxRetries")).isEqualTo(AIChatModelFactory.CHAT_MODEL_MAX_RETRIES);
    }

    private Object fieldValue(Object target, String fieldName) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
