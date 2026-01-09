package org.freeplane.plugin.ai.tools;

import java.util.Objects;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.IconDescription;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.UserIcon;

final class IconDescriptionResolver {
    private static final String EMOJI_NAME_PREFIX = "emoji-";

    private final EnglishTextProvider englishTextProvider;

    IconDescriptionResolver(EnglishTextProvider englishTextProvider) {
        this.englishTextProvider = Objects.requireNonNull(englishTextProvider, "englishTextProvider");
    }

    String resolveDescription(NamedIcon icon) {
        if (icon == null) {
            return "";
        }
        String emoji = decodeEmojiFromName(icon.getName());
        if (!TextUtils.isEmpty(emoji)) {
            return emoji;
        }
        if (icon instanceof IconDescription) {
            String file = ((IconDescription) icon).getFile();
            if (!TextUtils.isEmpty(file) && icon instanceof UserIcon) {
                return file;
            }
        }
        String englishDescription = resolveEnglishDescription(icon);
        if (!TextUtils.isEmpty(englishDescription)) {
            return englishDescription;
        }
        return fallbackName(icon.getName());
    }

    boolean matchesDescription(NamedIcon icon, String description) {
        if (icon == null || TextUtils.isEmpty(description)) {
            return false;
        }
        String normalizedDescription = description.trim();
        if (normalizedDescription.equalsIgnoreCase(icon.getName())) {
            return true;
        }
        String resolved = resolveDescription(icon);
        if (normalizedDescription.equals(resolved)) {
            return true;
        }
        return normalizedDescription.equalsIgnoreCase(resolved);
    }

    private String resolveEnglishDescription(NamedIcon icon) {
        if (icon instanceof IconDescription) {
            String translationKey = ((IconDescription) icon).getDescriptionTranslationKey();
            if (!TextUtils.isEmpty(translationKey)) {
                return englishTextProvider.getEnglishText(translationKey);
            }
        }
        return null;
    }

    private String fallbackName(String name) {
        if (TextUtils.isEmpty(name)) {
            return "";
        }
        String trimmedName = name;
        int index = name.lastIndexOf('/');
        if (index >= 0 && index + 1 < name.length()) {
            trimmedName = name.substring(index + 1);
        }
        return TextUtils.capitalize(trimmedName);
    }

    private String decodeEmojiFromName(String name) {
        if (!TextUtils.isEmpty(name) && name.startsWith(EMOJI_NAME_PREFIX)) {
            String codePointsPart = name.substring(EMOJI_NAME_PREFIX.length());
            if (!TextUtils.isEmpty(codePointsPart)) {
                String[] parts = codePointsPart.split("-");
                int[] codePoints = new int[parts.length];
                for (int index = 0; index < parts.length; index++) {
                    String part = parts[index];
                    if (TextUtils.isEmpty(part)) {
                        return null;
                    }
                    try {
                        codePoints[index] = Integer.parseInt(part, 16);
                    } catch (NumberFormatException exception) {
                        return null;
                    }
                }
                return new String(codePoints, 0, codePoints.length);
            }
        }
        return null;
    }
}
