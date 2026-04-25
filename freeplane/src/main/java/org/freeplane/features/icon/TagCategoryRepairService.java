package org.freeplane.features.icon;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.Controller;

public class TagCategoryRepairService {
    public static class RepairResult {
        private final List<String> changes;

        RepairResult(List<String> changes) {
            this.changes = changes;
        }

        public boolean hasChanges() {
            return !changes.isEmpty();
        }

        public String toMessage() {
            if (!hasChanges()) {
                return "";
            }
            return translatedText("tag_category_repair_report_prefix", "Legacy tag names were normalized:")
                + System.lineSeparator()
                + String.join(System.lineSeparator(), changes);
        }
    }

    private static class Replacement {
        private final String from;
        private final String to;

        Replacement(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Replacement)) {
                return false;
            }
            Replacement replacement = (Replacement) other;
            return from.equals(replacement.from) && to.equals(replacement.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

    private final LinkedHashSet<Replacement> replacements = new LinkedHashSet<>();

    public String replaceBoundaryWhitespace(String value) {
        String original = value == null ? "" : value;
        String normalized;
        if (original.isEmpty()) {
            normalized = "_";
        }
        else {
            char[] chars = original.toCharArray();
            int leading = 0;
            while (leading < chars.length && chars[leading] <= ' ') {
                chars[leading] = '_';
                leading++;
            }
            for (int trailing = chars.length - 1; trailing >= 0 && chars[trailing] <= ' '; trailing--) {
                chars[trailing] = '_';
            }
            normalized = new String(chars);
        }
        if (!original.equals(normalized)) {
            replacements.add(new Replacement(original, normalized));
        }
        return normalized;
    }

    public RepairResult repairResult() {
        ArrayList<String> changes = new ArrayList<>(replacements.size());
        for (Replacement replacement : replacements) {
            changes.add(changeDescription(replacement.from, replacement.to));
        }
        return new RepairResult(changes);
    }

    private String changeDescription(String from, String to) {
        return translatedFormat("tag_category_repair_report_renamed", "\"{0}\" -> \"{1}\"", from, to);
    }

    private static String translatedText(String key, String fallback) {
        return Controller.getCurrentController() == null ? fallback : TextUtils.getText(key);
    }

    private static String translatedFormat(String key, String fallbackPattern, Object... arguments) {
        String pattern = Controller.getCurrentController() == null ? fallbackPattern : TextUtils.getText(key);
        return new MessageFormat(pattern).format(arguments);
    }
}
