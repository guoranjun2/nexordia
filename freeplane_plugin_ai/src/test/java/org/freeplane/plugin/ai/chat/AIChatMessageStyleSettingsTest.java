package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.freeplane.core.resources.ResourceController;
import org.junit.Test;

public class AIChatMessageStyleSettingsTest {

    @Test
    public void parseChatFontSizeReturnsDefaultWhenValueMissing() {
        assertThat(AIChatMessageStyleSettings.parseChatFontSize(null)).isEqualTo(12);
    }

    @Test
    public void parseChatFontSizeReturnsDefaultWhenValueIsInvalid() {
        assertThat(AIChatMessageStyleSettings.parseChatFontSize("abc")).isEqualTo(12);
        assertThat(AIChatMessageStyleSettings.parseChatFontSize("-1")).isEqualTo(12);
        assertThat(AIChatMessageStyleSettings.parseChatFontSize("0")).isEqualTo(12);
    }

    @Test
    public void parseChatFontSizeReturnsParsedValueWhenPositive() {
        assertThat(AIChatMessageStyleSettings.parseChatFontSize("16")).isEqualTo(16);
    }

    @Test
    public void settingsReadFontSizeFromResourceController() {
        ResourceController resourceController = mock(ResourceController.class);
        when(resourceController.getProperty(AIChatMessageStyleSettings.CHAT_FONT_SIZE_PROPERTY))
            .thenReturn("18");

        AIChatMessageStyleSettings uut = new AIChatMessageStyleSettings(resourceController);

        assertThat(uut.getChatFontSize()).isEqualTo(18);
    }
}
