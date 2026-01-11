package org.freeplane.plugin.ai.chat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SystemMessageBuilderTest {
    @Test
    public void buildForChat_returnsConfiguredMessage() {
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> "Use tools when needed.";
        SystemMessageBuilder builder = new SystemMessageBuilder(textProvider);

        String message = builder.buildForChat();

        assertThat(message).isEqualTo("Use tools when needed.\n\n"
            + "Any tool calls in this chat require arguments wrapped under the single parameter named request. "
            + "Example: tool({ \"request\": { ... } })");
    }

    @Test
    public void buildForChat_returnsEmptyWhenBlank() {
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> "  ";
        SystemMessageBuilder builder = new SystemMessageBuilder(textProvider);

        String message = builder.buildForChat();

        assertThat(message).isEqualTo("Any tool calls in this chat require arguments wrapped under the single parameter named request. "
            + "Example: tool({ \"request\": { ... } })");
    }

    @Test
    public void buildForChat_returnsEmptyWhenNull() {
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> null;
        SystemMessageBuilder builder = new SystemMessageBuilder(textProvider);

        String message = builder.buildForChat();

        assertThat(message).isEqualTo("Any tool calls in this chat require arguments wrapped under the single parameter named request. "
            + "Example: tool({ \"request\": { ... } })");
    }
}
