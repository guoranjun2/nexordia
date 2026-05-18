package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class PromptToolSelectionResolverTest {
    @Test
    public void resolveEffectiveToolAvailability_usesCurrentSetting_whenSelectionIsBlank() {
        ChatToolAvailabilitySettings settings = mock(ChatToolAvailabilitySettings.class);
        when(settings.getToolAvailability()).thenReturn(ChatToolAvailability.READING);
        PromptToolSelectionResolver uut = new PromptToolSelectionResolver(settings);

        assertThat(uut.resolveEffectiveToolAvailability("  ")).isEqualTo(ChatToolAvailability.READING);
        assertThat(uut.resolveShownChatOverride("  ")).isNull();
    }

    @Test
    public void resolveEffectiveToolAvailability_usesPromptSelection_whenSpecified() {
        ChatToolAvailabilitySettings settings = mock(ChatToolAvailabilitySettings.class);
        when(settings.getToolAvailability()).thenReturn(ChatToolAvailability.READING);
        PromptToolSelectionResolver uut = new PromptToolSelectionResolver(settings);

        assertThat(uut.resolveEffectiveToolAvailability("disabled")).isEqualTo(ChatToolAvailability.DISABLED);
        assertThat(uut.resolveShownChatOverride("disabled")).isEqualTo(ChatToolAvailability.DISABLED);
    }

    @Test
    public void resolveEffectiveToolAvailability_defaultsInvalidSelectionToEditing() {
        ChatToolAvailabilitySettings settings = mock(ChatToolAvailabilitySettings.class);
        when(settings.getToolAvailability()).thenReturn(ChatToolAvailability.READING);
        PromptToolSelectionResolver uut = new PromptToolSelectionResolver(settings);

        assertThat(uut.resolveEffectiveToolAvailability("unexpected")).isEqualTo(ChatToolAvailability.EDITING);
        assertThat(uut.resolveShownChatOverride("unexpected")).isEqualTo(ChatToolAvailability.EDITING);
    }
}
