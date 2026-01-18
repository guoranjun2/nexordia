package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SystemMessageBuilderTest {
    private static final String MARKDOWN_RESPONSE_GUIDANCE = "Respond in Markdown.";
    private static final String TOOL_CALL_GUIDANCE_TEXT =
        "Any tool calls in this chat require arguments wrapped under the single parameter named request. ";
    private static final String TOOL_CALL_EXAMPLE_TEXT = "Example: tool({ \"request\": { ... } })";

    @Test
    public void buildForChat_returnsConfiguredMessage() {
        String configuredMessage = "Use tools when needed.";
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> configuredMessage;
        SystemMessageBuilder uut = new SystemMessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).contains(configuredMessage);
        assertThat(message).contains(MARKDOWN_RESPONSE_GUIDANCE);
        assertThat(message).contains(TOOL_CALL_GUIDANCE_TEXT);
        assertThat(message).contains(TOOL_CALL_EXAMPLE_TEXT);
    }

    @Test
    public void buildForChat_returnsEmptyWhenBlank() {
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> "  ";
        SystemMessageBuilder uut = new SystemMessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).contains(MARKDOWN_RESPONSE_GUIDANCE);
        assertThat(message).contains(TOOL_CALL_GUIDANCE_TEXT);
        assertThat(message).contains(TOOL_CALL_EXAMPLE_TEXT);
    }

    @Test
    public void buildForChat_returnsEmptyWhenNull() {
        SystemMessageBuilder.SystemMessageTextProvider textProvider = () -> null;
        SystemMessageBuilder uut = new SystemMessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).contains(MARKDOWN_RESPONSE_GUIDANCE);
        assertThat(message).contains(TOOL_CALL_GUIDANCE_TEXT);
        assertThat(message).contains(TOOL_CALL_EXAMPLE_TEXT);
    }
}
