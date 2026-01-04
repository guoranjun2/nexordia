package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.IconDescription;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.UserIcon;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.styles.LogicalStyleController.StyleOption;

public class IconsContentReader {
    private static final String EMOJI_NAME_PREFIX = "emoji-";

    private final EnglishTextProvider englishTextProvider;
    private final IconController iconController;

    public IconsContentReader(EnglishTextProvider englishTextProvider, IconController iconController) {
        this.englishTextProvider = Objects.requireNonNull(englishTextProvider, "englishTextProvider");
        this.iconController = Objects.requireNonNull(iconController, "iconController");
    }

    public IconsContent readIconsContent(NodeModel nodeModel, NodeContentPreset preset) {
        if (nodeModel == null || preset == NodeContentPreset.BRIEF) {
            return null;
        }
        return buildIconsContent(nodeModel);
    }

    public IconsContent readIconsContent(NodeModel nodeModel, IconsContentRequest request) {
        if (nodeModel == null || request == null || !request.includesIcons()) {
            return null;
        }
        return buildIconsContent(nodeModel);
    }

    public List<String> collectSearchTerms(NodeModel nodeModel, IconsContentRequest request) {
        if (nodeModel == null || request == null || !request.includesIcons()) {
            return Collections.emptyList();
        }
        List<NamedIcon> icons = new ArrayList<>(iconController.getIcons(nodeModel, StyleOption.FOR_UNSELECTED_NODE));
        if (icons.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> terms = new ArrayList<>(icons.size() * 2);
        for (NamedIcon icon : icons) {
            String description = resolveDescription(icon);
            if (!TextUtils.isEmpty(description)) {
                terms.add(description);
            }
            if (isEmojiName(icon.getName())) {
                String descriptionKey = getDescriptionTranslationKey(icon);
                if (!TextUtils.isEmpty(descriptionKey) && !descriptionKey.equals(description)) {
                    terms.add(descriptionKey);
                }
            }
        }
        return terms;
    }

    public boolean matches(NodeModel nodeModel, IconsContentRequest request, NodeContentValueMatcher valueMatcher) {
        if (nodeModel == null || request == null || !request.includesIcons() || valueMatcher == null) {
            return false;
        }
        List<String> terms = collectSearchTerms(nodeModel, request);
        for (String term : terms) {
            if (valueMatcher.matchesValue(term)) {
                return true;
            }
        }
        return false;
    }

    private IconsContent buildIconsContent(NodeModel nodeModel) {
        List<NamedIcon> icons = new ArrayList<>(iconController.getIcons(nodeModel, StyleOption.FOR_UNSELECTED_NODE));
        if (icons.isEmpty()) {
            return null;
        }
        List<String> descriptions = new ArrayList<>(icons.size());
        for (NamedIcon icon : icons) {
            descriptions.add(resolveDescription(icon));
        }
        return new IconsContent(descriptions);
    }

    private String resolveDescription(NamedIcon icon) {
        if (icon == null) {
            return "";
        }
        String emoji = decodeEmojiFromName(icon.getName());
        if (!TextUtils.isEmpty(emoji)) {
            return emoji;
        }
        if (icon instanceof UserIcon) {
            String file = icon.getFile();
            if (!TextUtils.isEmpty(file)) {
                return file;
            }
        }
        String englishDescription = getEnglishDescription(icon);
        if (!TextUtils.isEmpty(englishDescription)) {
            return englishDescription;
        }
        return fallbackName(icon.getName());
    }

    private String getEnglishDescription(NamedIcon icon) {
        String descriptionKey = getDescriptionTranslationKey(icon);
        if (TextUtils.isEmpty(descriptionKey)) {
            return null;
        }
        return englishTextProvider.getEnglishText(descriptionKey);
    }

    private String getDescriptionTranslationKey(NamedIcon icon) {
        if (icon instanceof IconDescription) {
            return ((IconDescription) icon).getDescriptionTranslationKey();
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
        if (TextUtils.isEmpty(name) || !isEmojiName(name)) {
            return null;
        }
        String codePointsPart = name.substring(EMOJI_NAME_PREFIX.length());
        if (TextUtils.isEmpty(codePointsPart)) {
            return null;
        }
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

    private boolean isEmojiName(String name) {
        return !TextUtils.isEmpty(name) && name.startsWith(EMOJI_NAME_PREFIX);
    }
}
