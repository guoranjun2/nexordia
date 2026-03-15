package org.freeplane.features.icon;

import java.util.List;
import java.util.Objects;

public class TagCategoryDraftState {
    private final TagCategories tagCategories;
    private final TagCategoryState categoryState;

    private TagCategoryDraftState(TagCategories tagCategories) {
        this.tagCategories = Objects.requireNonNull(tagCategories, "tagCategories must not be null");
        this.categoryState = TagCategoryStateBuilder.from(tagCategories);
    }

    public static TagCategoryDraftState fromTagCategories(TagCategories tagCategories) {
        return new TagCategoryDraftState(tagCategories);
    }

    public TagCategories toTagCategories() {
        return tagCategories;
    }

    public String getCategorySeparator() {
        return categoryState.getCategorySeparator();
    }

    public List<TagCategoryNode> getCategories() {
        return categoryState.getCategories();
    }

    public List<TagItem> getUncategorizedTags() {
        return categoryState.getUncategorizedTags();
    }

    @Override
    public int hashCode() {
        return categoryState.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategoryDraftState)) {
            return false;
        }
        TagCategoryDraftState other = (TagCategoryDraftState) obj;
        return categoryState.equals(other.categoryState);
    }
}
