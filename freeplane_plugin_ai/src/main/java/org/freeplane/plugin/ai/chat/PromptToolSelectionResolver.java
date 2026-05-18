package org.freeplane.plugin.ai.chat;

class PromptToolSelectionResolver {
    private final ChatToolAvailabilitySettings chatToolAvailabilitySettings;

    PromptToolSelectionResolver(ChatToolAvailabilitySettings chatToolAvailabilitySettings) {
        this.chatToolAvailabilitySettings = chatToolAvailabilitySettings;
    }

    ChatToolAvailability resolveEffectiveToolAvailability(String toolAvailabilitySelectionValue) {
        String normalizedSelectionValue = normalizeSelectionValue(toolAvailabilitySelectionValue);
        return normalizedSelectionValue == null
            ? chatToolAvailabilitySettings.getToolAvailability()
            : ChatToolAvailability.fromPreferenceValue(normalizedSelectionValue);
    }

    ChatToolAvailability resolveShownChatOverride(String toolAvailabilitySelectionValue) {
        String normalizedSelectionValue = normalizeSelectionValue(toolAvailabilitySelectionValue);
        return normalizedSelectionValue == null
            ? null
            : ChatToolAvailability.fromPreferenceValue(normalizedSelectionValue);
    }

    private String normalizeSelectionValue(String selectionValue) {
        if (selectionValue == null) {
            return null;
        }
        String normalized = selectionValue.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
