package org.freeplane.plugin.ai.chat;

import java.util.Locale;

public enum ChatMemoryMode {
    DISABLED("disabled"),
    MESSAGE_WINDOW("message_window");

    private final String propertyValue;

    ChatMemoryMode(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String getPropertyValue() {
        return propertyValue;
    }

    public static ChatMemoryMode fromPropertyValue(String value) {
        if (value == null || value.isEmpty()) {
            return MESSAGE_WINDOW;
        }
        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        for (ChatMemoryMode mode : values()) {
            if (mode.propertyValue.equals(normalizedValue)) {
                return mode;
            }
        }
        return MESSAGE_WINDOW;
    }
}
