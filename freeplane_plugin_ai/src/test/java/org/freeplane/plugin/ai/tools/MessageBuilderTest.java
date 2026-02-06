package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class MessageBuilderTest {
    private static final String MARKDOWN_RESPONSE_GUIDANCE = "Respond in Markdown.";
    private static final String TOOL_CALL_GUIDANCE_TEXT =
        "Any tool calls in this chat require arguments wrapped under the single parameter named request. ";
    private static final String TOOL_CALL_EXAMPLE_TEXT = "Example: tool({ \"request\": { ... } })";
    private static final String CONTROL_INSTRUCTION_GUIDANCE_PREFIX =
        "Control instructions start with: " + MessageBuilder.CONTROL_INSTRUCTION_PREFIX;

    @Test
    public void buildForChat_returnsConfiguredMessage() {
        String configuredMessage = "Use tools when needed.";
        MessageBuilder.MessageTextProvider textProvider = () -> configuredMessage;
        MessageBuilder uut = new MessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).contains(configuredMessage);
        assertThat(message).contains(MARKDOWN_RESPONSE_GUIDANCE);
        assertThat(message).contains(TOOL_CALL_GUIDANCE_TEXT);
        assertThat(message).contains(TOOL_CALL_EXAMPLE_TEXT);
        assertThat(message).contains(CONTROL_INSTRUCTION_GUIDANCE_PREFIX);
    }

    @Test
    public void buildForChat_returnsEmptyWhenBlank() {
        MessageBuilder.MessageTextProvider textProvider = () -> "  ";
        MessageBuilder uut = new MessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).contains(MARKDOWN_RESPONSE_GUIDANCE);
        assertThat(message).contains(TOOL_CALL_GUIDANCE_TEXT);
        assertThat(message).contains(TOOL_CALL_EXAMPLE_TEXT);
        assertThat(message).contains(CONTROL_INSTRUCTION_GUIDANCE_PREFIX);
    }

    @Test
    public void buildForChat_returnsEmptyWhenNull() {
        MessageBuilder.MessageTextProvider textProvider = () -> null;
        MessageBuilder uut = new MessageBuilder(textProvider);

        String message = uut.buildForChat();

        assertThat(message).contains(MARKDOWN_RESPONSE_GUIDANCE);
        assertThat(message).contains(TOOL_CALL_GUIDANCE_TEXT);
        assertThat(message).contains(TOOL_CALL_EXAMPLE_TEXT);
        assertThat(message).contains(CONTROL_INSTRUCTION_GUIDANCE_PREFIX);
    }

    @Test
    public void buildAssistantProfileInstruction_returnsNameAndDefinition() {
        String message = MessageBuilder.buildAssistantProfileInstruction("Analyst", "Be strict.", true);

        assertThat(message).isEqualTo("Now you have the profile Analyst.\nProfile definition: Be strict.");
    }

    @Test
    public void buildAssistantProfileInstruction_returnsFallbackForEmptyDefinition() {
        String message = MessageBuilder.buildAssistantProfileInstruction("Analyst", "  ", true);

        assertThat(message).isEqualTo("Now you have the profile Analyst.");
    }

    @Test
    public void buildAssistantProfileInstruction_nonCurrentProfile_keepsOnlyProfileName() {
        String historical = MessageBuilder.buildAssistantProfileInstruction("Analyst", "Be strict.", false);

        assertThat(historical).isEqualTo("Now you have the profile Analyst.");
    }

}
