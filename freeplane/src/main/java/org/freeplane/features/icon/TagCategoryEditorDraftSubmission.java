package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagCategoryEditorDraftSubmission {
    private final String expectedRevision;
    private final TagCategories draftCategories;
    private final List<String> replacementPairs;

    public TagCategoryEditorDraftSubmission(String expectedRevision,
                                            TagCategories draftCategories,
                                            List<String> replacementPairs) {
        if (isBlank(expectedRevision)) {
            throw new IllegalArgumentException("expectedRevision must not be blank");
        }
        if (draftCategories == null) {
            throw new IllegalArgumentException("draftCategories must not be null");
        }
        if (replacementPairs == null) {
            throw new IllegalArgumentException("replacementPairs must not be null");
        }
        if ((replacementPairs.size() % 2) != 0) {
            throw new IllegalArgumentException("replacementPairs must contain old/new pairs");
        }
        this.expectedRevision = expectedRevision;
        this.draftCategories = draftCategories;
        this.replacementPairs = copyReplacementPairs(replacementPairs);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<String> copyReplacementPairs(List<String> source) {
        ArrayList<String> copy = new ArrayList<>(source.size());
        for (String replacement : source) {
            if (replacement == null) {
                throw new IllegalArgumentException("replacementPairs must not contain null");
            }
            copy.add(replacement);
        }
        return Collections.unmodifiableList(copy);
    }

    public void requireMatchingRevision(String currentRevision) {
        if (!expectedRevision.equals(currentRevision)) {
            throw new TagCategoryConflictException(
                "stale revision: expected " + expectedRevision + ", current " + currentRevision);
        }
    }

    public String getExpectedRevision() {
        return expectedRevision;
    }

    public TagCategories getDraftCategories() {
        return draftCategories;
    }

    public List<String> getReplacementPairs() {
        return replacementPairs;
    }
}
