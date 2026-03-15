package org.freeplane.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MapTagCategoryState {
    private final String revision;
    private final String categorySeparator;
    private final List<MapTagCategoryNode> categories;
    private final List<MapTagItem> uncategorizedTags;

    public MapTagCategoryState(String revision,
                               String categorySeparator,
                               List<MapTagCategoryNode> categories,
                               List<MapTagItem> uncategorizedTags) {
        if (revision == null || revision.trim().isEmpty()) {
            throw new IllegalArgumentException("revision must not be blank");
        }
        if (categorySeparator == null || categorySeparator.trim().isEmpty()) {
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

    public String getRevision() {
        return revision;
    }

    public String getCategorySeparator() {
        return categorySeparator;
    }

    public List<MapTagCategoryNode> getCategories() {
        return categories;
    }

    public List<MapTagItem> getUncategorizedTags() {
        return uncategorizedTags;
    }

    @Override
    public int hashCode() {
        return Objects.hash(revision, categorySeparator, categories, uncategorizedTags);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapTagCategoryState)) {
            return false;
        }
        MapTagCategoryState other = (MapTagCategoryState) obj;
        return Objects.equals(revision, other.revision)
            && Objects.equals(categorySeparator, other.categorySeparator)
            && Objects.equals(categories, other.categories)
            && Objects.equals(uncategorizedTags, other.uncategorizedTags);
    }
}
