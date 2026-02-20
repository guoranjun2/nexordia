package org.freeplane.plugin.ai.tools.tagcategories;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.freeplane.features.icon.TagCategoryNode;
import org.freeplane.features.icon.TagCategorySnapshot;
import org.freeplane.features.icon.TagDescriptor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagCategorySnapshotPayload {
    private final String mapIdentifier;
    private final String revision;
    private final String separator;
    private final List<TagCategoryNodePayload> categories;
    private final List<TagDescriptorPayload> uncategorizedTags;

    @JsonCreator
    public TagCategorySnapshotPayload(@JsonProperty("mapIdentifier") String mapIdentifier,
                                      @JsonProperty("revision") String revision,
                                      @JsonProperty("separator") String separator,
                                      @JsonProperty("categories") List<TagCategoryNodePayload> categories,
                                      @JsonProperty("uncategorizedTags") List<TagDescriptorPayload> uncategorizedTags) {
        this.mapIdentifier = mapIdentifier;
        this.revision = revision;
        this.separator = separator;
        this.categories = categories;
        this.uncategorizedTags = uncategorizedTags;
    }

    public static TagCategorySnapshotPayload fromSnapshot(String mapIdentifier, TagCategorySnapshot snapshot) {
        ArrayList<TagCategoryNodePayload> categoryPayloads = new ArrayList<>();
        for (TagCategoryNode category : snapshot.getCategories()) {
            categoryPayloads.add(TagCategoryNodePayload.fromNode(category));
        }
        ArrayList<TagDescriptorPayload> uncategorizedPayloads = new ArrayList<>();
        for (TagDescriptor uncategorizedTag : snapshot.getUncategorizedTags()) {
            uncategorizedPayloads.add(TagDescriptorPayload.fromDescriptor(uncategorizedTag));
        }
        return new TagCategorySnapshotPayload(
            mapIdentifier,
            snapshot.getRevision(),
            snapshot.getSeparator(),
            categoryPayloads,
            uncategorizedPayloads);
    }

    public TagCategorySnapshot toSnapshot() {
        ArrayList<TagCategoryNode> categoryNodes = new ArrayList<>();
        if (categories != null) {
            for (TagCategoryNodePayload category : categories) {
                categoryNodes.add(category.toNode());
            }
        }
        ArrayList<TagDescriptor> uncategorizedDescriptors = new ArrayList<>();
        if (uncategorizedTags != null) {
            for (TagDescriptorPayload uncategorizedTag : uncategorizedTags) {
                uncategorizedDescriptors.add(uncategorizedTag.toDescriptor());
            }
        }
        return new TagCategorySnapshot(revision, separator, categoryNodes, uncategorizedDescriptors);
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getRevision() {
        return revision;
    }

    public String getSeparator() {
        return separator;
    }

    public List<TagCategoryNodePayload> getCategories() {
        return categories;
    }

    public List<TagDescriptorPayload> getUncategorizedTags() {
        return uncategorizedTags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapIdentifier, revision, separator, categories, uncategorizedTags);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategorySnapshotPayload)) {
            return false;
        }
        TagCategorySnapshotPayload other = (TagCategorySnapshotPayload) obj;
        return Objects.equals(mapIdentifier, other.mapIdentifier)
            && Objects.equals(revision, other.revision)
            && Objects.equals(separator, other.separator)
            && Objects.equals(categories, other.categories)
            && Objects.equals(uncategorizedTags, other.uncategorizedTags);
    }
}
