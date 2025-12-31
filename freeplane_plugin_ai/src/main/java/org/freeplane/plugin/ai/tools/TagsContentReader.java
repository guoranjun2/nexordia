package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.freeplane.features.icon.IconController;
import org.freeplane.features.icon.Tag;
import org.freeplane.features.map.NodeModel;

public class TagsContentReader {
    private final IconController iconController;

    public TagsContentReader(IconController iconController) {
        this.iconController = Objects.requireNonNull(iconController, "iconController");
    }

    public TagsContent readTagsContent(NodeModel nodeModel, NodeContentPreset preset) {
        if (nodeModel == null || preset == NodeContentPreset.BRIEF) {
            return null;
        }
        List<Tag> tags = iconController.getTags(nodeModel);
        if (tags.isEmpty()) {
            return null;
        }
        List<String> tagNames = new ArrayList<>(tags.size());
        for (Tag tag : tags) {
            tagNames.add(tag.getContent());
        }
        return new TagsContent(tagNames);
    }
}
