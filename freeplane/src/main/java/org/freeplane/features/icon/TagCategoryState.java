package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagCategoryState {
    private final String revision;
    private final String categorySeparator;
    private final List<TagCategoryNode> categories;
    private final List<TagItem> uncategorizedTags;

    public TagCategoryState(String revision,
                            String categorySeparator,
                            List<TagCategoryNode> categories,
                            List<TagItem> uncategorizedTags) {
        if (isBlank(revision)) {
            throw new IllegalArgumentException("revision must not be blank");
        }
        if (isBlank(categorySeparator)) {
            throw new IllegalArgumentException("categorySeparator must not be blank");
        }
        if (categories == null) {
            throw new IllegalArgumentException("categories must not be null");
        }
        if (uncategorizedTags == null) {
            throw new IllegalArgumentException("uncategorizedTags must not be null");
        }
        this.revision = revision;
        this.categorySeparator = categorySeparator;
        this.categories = Collections.unmodifiableList(new ArrayList<>(categories));
        this.uncategorizedTags = Collections.unmodifiableList(new ArrayList<>(uncategorizedTags));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public String getRevision() {
        return revision;
    }

    public String getCategorySeparator() {
        return categorySeparator;
    }

    public List<TagCategoryNode> getCategories() {
        return categories;
    }

    public List<TagItem> getUncategorizedTags() {
        return uncategorizedTags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, categorySeparator, categories, uncategorizedTags);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategoryState)) {
            return false;
        }
        TagCategoryState other = (TagCategoryState) obj;
        return Objects.equals(revision, other.revision)
            && Objects.equals(categorySeparator, other.categorySeparator)
            && Objects.equals(categories, other.categories)
            && Objects.equals(uncategorizedTags, other.uncategorizedTags);
    }
}
