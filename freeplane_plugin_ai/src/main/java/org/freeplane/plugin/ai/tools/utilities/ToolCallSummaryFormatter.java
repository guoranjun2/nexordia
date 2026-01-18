package org.freeplane.plugin.ai.tools.utilities;

import java.util.Collection;
import java.util.Iterator;

public class ToolCallSummaryFormatter {
    private static final int MAXIMUM_SUMMARY_TEXT_LENGTH = 160;

    private ToolCallSummaryFormatter() {
    }

    public static String sanitizeValue(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\r\n", " ").replace("\n", " ").replace("\r", " ").trim();
        if (normalized.length() <= MAXIMUM_SUMMARY_TEXT_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAXIMUM_SUMMARY_TEXT_LENGTH - 3) + "...";
    }

    public static String joinEnumValues(Collection<? extends Enum<?>> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        Iterator<? extends Enum<?>> iterator = values.iterator();
        while (iterator.hasNext()) {
            builder.append(iterator.next().name());
            if (iterator.hasNext()) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    public static String joinTextValues(Collection<String> values, String separator) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        boolean hasValue = false;
        for (String value : values) {
            String sanitizedValue = sanitizeValue(value);
            if (sanitizedValue.isEmpty()) {
                continue;
            }
            if (hasValue) {
                builder.append(separator);
            }
            builder.append(sanitizedValue);
            hasValue = true;
        }
        return builder.toString();
    }
}
