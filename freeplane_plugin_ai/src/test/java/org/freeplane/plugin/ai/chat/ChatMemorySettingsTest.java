package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.freeplane.core.resources.ResourceController;
import org.junit.Test;

public class ChatMemorySettingsTest {

    @Test
    public void parsesTokenLimitFromProperties() {
        ResourceController resourceController = mock(ResourceController.class);
        when(resourceController.getProperty("ai_chat_memory_maximum_token_count")).thenReturn("2048");

        ChatMemorySettings uut = new ChatMemorySettings(resourceController);

        assertThat(uut.getMaximumTokenCount()).isEqualTo(2048);
    }

    @Test
    public void fallsBackToDefaultsForMissingOrInvalidValues() {
        ResourceController resourceController = mock(ResourceController.class);
        when(resourceController.getProperty("ai_chat_memory_maximum_token_count")).thenReturn("-5");

        ChatMemorySettings uut = new ChatMemorySettings(resourceController);

        assertThat(uut.getMaximumTokenCount()).isEqualTo(65536);
    }
}
