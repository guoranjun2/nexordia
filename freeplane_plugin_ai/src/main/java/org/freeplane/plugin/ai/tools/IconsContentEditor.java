package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.map.NodeModel;

public final class IconsContentEditor {
    private final IconDescriptionResolver iconDescriptionResolver;
    private final Iterable<NamedIcon> defaultCandidates;

    public IconsContentEditor(IconDescriptionResolver iconDescriptionResolver, Iterable<NamedIcon> defaultCandidates) {
        this.iconDescriptionResolver = Objects.requireNonNull(iconDescriptionResolver, "iconDescriptionResolver");
        this.defaultCandidates = Objects.requireNonNull(defaultCandidates, "defaultCandidates");
    }

    public void apply(NodeModel nodeModel, IconsContent iconsContent) {
        if (nodeModel == null || iconsContent == null) {
            return;
        }
        List<String> descriptions = iconsContent.getDescriptions();
        if (descriptions == null || descriptions.isEmpty()) {
            return;
        }
        IconRegistry iconRegistry = nodeModel.getMap().getIconRegistry();
        List<NamedIcon> candidates = collectCandidateIcons(iconRegistry);
        if (candidates.isEmpty()) {
            return;
        }
        Set<String> addedNames = new HashSet<>();
        for (String description : descriptions) {
            if (TextUtils.isEmpty(description)) {
                continue;
            }
            NamedIcon icon = findMatchingIcon(candidates, description.trim());
            if (icon != null && addedNames.add(icon.getName())) {
                nodeModel.addIcon(icon);
            }
        }
    }

    private List<NamedIcon> collectCandidateIcons(IconRegistry iconRegistry) {
        List<NamedIcon> candidates = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        addIcons(candidates, seenNames, defaultCandidates);
        if (iconRegistry != null) {
            Iterator<NamedIcon> iterator = iconRegistry.getIconsAsListModel().iterator();
            addIcons(candidates, seenNames, () -> iterator);
        }
        return candidates;
    }

    private void addIcons(List<NamedIcon> target, Set<String> seenNames, Iterable<NamedIcon> source) {
        if (source == null) {
            return;
        }
        for (NamedIcon icon : source) {
            addIcon(target, seenNames, icon);
        }
    }

    private void addIcons(List<NamedIcon> target, Set<String> seenNames, Iterator<NamedIcon> iterator) {
        if (iterator == null) {
            return;
        }
        iterator.forEachRemaining(icon -> addIcon(target, seenNames, icon));
    }

    private void addIcon(List<NamedIcon> target, Set<String> seenNames, NamedIcon icon) {
        if (icon == null || !seenNames.add(icon.getName())) {
            return;
        }
        target.add(icon);
    }

    private NamedIcon findMatchingIcon(List<NamedIcon> candidates, String description) {
        if (description == null) {
            return null;
        }
        for (NamedIcon candidate : candidates) {
            if (iconDescriptionResolver.matchesDescription(candidate, description)) {
                return candidate;
            }
        }
        return null;
    }
}
