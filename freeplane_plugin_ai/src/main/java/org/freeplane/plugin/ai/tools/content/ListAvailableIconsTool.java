package org.freeplane.plugin.ai.tools.content;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.factory.IconStoreFactory;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummary;
import org.freeplane.plugin.ai.tools.utilities.ToolCallSummaryFormatter;

public class ListAvailableIconsTool {
    private static final String NOTE_MESSAGE = "This list includes built-in and user-defined Freeplane icons only; "
        + "emoji icons are referenced by the emoji character itself and are not listed here.";

    private final IconDescriptionResolver iconDescriptionResolver;

    public ListAvailableIconsTool(IconDescriptionResolver iconDescriptionResolver) {
        this.iconDescriptionResolver = Objects.requireNonNull(iconDescriptionResolver, "iconDescriptionResolver");
    }

    public ListAvailableIconsResponse listAvailableIcons() {
        Collection<MindIcon> icons = IconStoreFactory.ICON_STORE.getMindIcons();
        Set<String> descriptions = new LinkedHashSet<>();
        for (NamedIcon icon : icons) {
            if (icon == null || isEmojiIcon(icon)) {
                continue;
            }
            String description = iconDescriptionResolver.resolveDescription(icon);
            if (description != null && !description.trim().isEmpty()) {
                descriptions.add(description);
            }
        }
        List<String> iconsList = new ArrayList<>(descriptions);
        return new ListAvailableIconsResponse(iconsList, NOTE_MESSAGE);
    }

    public ToolCallSummary buildToolCallSummary(ListAvailableIconsResponse response) {
        int iconCount = response == null || response.getIcons() == null ? 0 : response.getIcons().size();
        String summaryText = "listAvailableIcons: icons=" + iconCount;
        return new ToolCallSummary("listAvailableIcons", summaryText, false);
    }

    public ToolCallSummary buildToolCallErrorSummary(RuntimeException error) {
        String message = error == null ? "Unknown error" : error.getMessage();
        String safeMessage = ToolCallSummaryFormatter.sanitizeValue(message == null
            ? error.getClass().getSimpleName()
            : message);
        return new ToolCallSummary("listAvailableIcons", "listAvailableIcons error: " + safeMessage, true);
    }

    private boolean isEmojiIcon(NamedIcon icon) {
        String name = icon.getName();
        return name != null && name.startsWith("emoji-");
    }
}
