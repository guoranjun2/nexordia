package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SystemMessageBuilderTest {
    @Test
    public void buildForChat_returnsConfiguredMessage() {
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> "Use tools when needed.";
        SystemMessageBuilder uut = new SystemMessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).isEqualTo("Use tools when needed.");
    }

    @Test
    public void buildForChat_returnsEmptyWhenBlank() {
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> "  ";
        SystemMessageBuilder uut = new SystemMessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).isNull();
    }

    @Test
    public void buildForChat_returnsEmptyWhenNull() {
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> null;
        SystemMessageBuilder uut = new SystemMessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).isNull();
    }
}
