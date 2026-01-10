package org.freeplane.plugin.ai.tools;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagReference;
import org.freeplane.features.icon.Tags;
import org.freeplane.features.map.NodeModel;

public class TagsContentEditor {
    public void apply(NodeModel nodeModel, TagsContent tagsContent) {
        if (nodeModel == null || tagsContent == null) {
            return;
        }
        List<String> tags = tagsContent.getTags();
        if (tags == null || tags.isEmpty()) {
            return;
        }
        TagCategories tagCategories = nodeModel.getMap().getIconRegistry().getTagCategories();
        if (tagCategories == null) {
            return;
        }
        List<TagReference> references = new ArrayList<>();
        Set<String> texts = new HashSet<>();
        for (String tagText : tags) {
            if (TextUtils.isEmpty(tagText) || !texts.add(tagText.trim())) {
                continue;
            }
            references.add(tagCategories.createTagReference(tagText));
        }
        if (!references.isEmpty()) {
            Tags.setTagReferences(nodeModel, references);
        }
    }
}
