/*
 * Created on 2 Apr 2024
 *
 * author dimitry
 */
package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.freeplane.core.extension.IExtension;
import org.freeplane.features.map.NodeModel;

public class Tags implements IExtension {
    private List<TagReference> tags;

    private Tags(List<TagReference> tags) {
        super();
        this.tags = tags;
    }

    public static List<TagReference> getTagReferences(NodeModel node){
        Tags tags = node.getExtension(Tags.class);
        return (tags == null) ? Collections.emptyList() : tags.tags;
    }

    public static List<TagReference> getExistingTagReferences(NodeModel node) {
        Tags tags = node.getExtension(Tags.class);
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags.getExistingTagReferences();
    }

    public static void setTagReferences(NodeModel node, List<TagReference> newTags) {
        Tags extension = node.getExtension(Tags.class);
        if(extension == null) {
            extension = new Tags(newTags);
            node.addExtension(extension);
        }
        else
            extension.tags = newTags;
    }

    public List<TagReference> getTagReferencess(){
        return Collections.unmodifiableList(tags);
    }

    public List<TagReference> getExistingTagReferences() {
        ArrayList<TagReference> existingTagReferences = new ArrayList<>(tags.size());
        for (TagReference reference : tags) {
            if (reference == null || reference.exists()) {
                existingTagReferences.add(reference);
            }
        }
        return Collections.unmodifiableList(existingTagReferences);
    }

    public List<Tag> getTags() {
        Set<String>  duplicates = new HashSet<>();
        return tags
                .stream()
                .map(TagReference::getTag)
                .filter(x -> x != Tag.REMOVED_TAG)
                .filter(x -> x.isEmpty() || duplicates.add(x.getContent()))
                .collect(Collectors.toList());
    }

}
