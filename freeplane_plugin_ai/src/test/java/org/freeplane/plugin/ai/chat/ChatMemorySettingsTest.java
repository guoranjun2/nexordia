package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ChatMemorySettingsTest {

    @Test
    public void parseMaximumTokenCountDefaultsOnEmptyInput() {
        assertThat(ChatMemorySettings.parseMaximumTokenCount(null)).isEqualTo(65536);
        assertThat(ChatMemorySettings.parseMaximumTokenCount("")).isEqualTo(65536);
        assertThat(ChatMemorySettings.parseMaximumTokenCount("   ")).isEqualTo(65536);
    }

    @Test
    public void parseMaximumTokenCountDefaultsOnInvalidInput() {
        assertThat(ChatMemorySettings.parseMaximumTokenCount("nope")).isEqualTo(65536);
        assertThat(ChatMemorySettings.parseMaximumTokenCount("-5")).isEqualTo(65536);
        assertThat(ChatMemorySettings.parseMaximumTokenCount("0")).isEqualTo(65536);
    }

    @Test
    public void parseMaximumTokenCountParsesPositiveInput() {
        assertThat(ChatMemorySettings.parseMaximumTokenCount("8192")).isEqualTo(8192);
        assertThat(ChatMemorySettings.parseMaximumTokenCount(" 9000 ")).isEqualTo(9000);
        assertThat(ChatMemorySettings.parseMaximumTokenCount("3000")).isEqualTo(3000);
    }
}
