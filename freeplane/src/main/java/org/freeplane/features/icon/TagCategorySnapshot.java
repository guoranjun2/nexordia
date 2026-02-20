package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagCategorySnapshot {
    private final String revision;
    private final String separator;
    private final List<TagCategoryNode> categories;
    private final List<TagDescriptor> uncategorizedTags;

    public TagCategorySnapshot(String revision,
                               String separator,
                               List<TagCategoryNode> categories,
                               List<TagDescriptor> uncategorizedTags) {
        if (isBlank(revision)) {
            throw new IllegalArgumentException("revision must not be blank");
        }
        if (isBlank(separator)) {
            throw new IllegalArgumentException("separator must not be blank");
        }
        if (categories == null) {
            throw new IllegalArgumentException("categories must not be null");
        }
        if (uncategorizedTags == null) {
            throw new IllegalArgumentException("uncategorizedTags must not be null");
        }
        this.revision = revision;
        this.separator = separator;
        this.categories = Collections.unmodifiableList(new ArrayList<>(categories));
        this.uncategorizedTags = Collections.unmodifiableList(new ArrayList<>(uncategorizedTags));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public String getRevision() {
        return revision;
    }

    public String getSeparator() {
        return separator;
    }

    public List<TagCategoryNode> getCategories() {
        return categories;
    }

    public List<TagDescriptor> getUncategorizedTags() {
        return uncategorizedTags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, separator, categories, uncategorizedTags);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategorySnapshot)) {
            return false;
        }
        TagCategorySnapshot other = (TagCategorySnapshot) obj;
        return Objects.equals(revision, other.revision)
            && Objects.equals(separator, other.separator)
            && Objects.equals(categories, other.categories)
            && Objects.equals(uncategorizedTags, other.uncategorizedTags);
    }
}
