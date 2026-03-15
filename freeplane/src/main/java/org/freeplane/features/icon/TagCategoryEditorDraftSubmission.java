package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TagCategoryEditorDraftSubmission {
    private final String expectedRevision;
    private final TagCategoryDraftState draftState;
    private final List<TagReferenceRewrite> referenceRewrites;

    public TagCategoryEditorDraftSubmission(String expectedRevision,
                                            TagCategoryDraftState draftState,
                                            List<TagReferenceRewrite> referenceRewrites) {
        if (isBlank(expectedRevision)) {
            throw new IllegalArgumentException("expectedRevision must not be blank");
        }
        if (draftState == null) {
            throw new IllegalArgumentException("draftState must not be null");
        }
        if (referenceRewrites == null) {
            throw new IllegalArgumentException("referenceRewrites must not be null");
        }
        this.expectedRevision = expectedRevision;
        this.draftState = draftState;
        this.referenceRewrites = copyReferenceRewrites(referenceRewrites);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<TagReferenceRewrite> copyReferenceRewrites(List<TagReferenceRewrite> source) {
        ArrayList<TagReferenceRewrite> copy = new ArrayList<>(source.size());
        for (TagReferenceRewrite referenceRewrite : source) {
            if (referenceRewrite == null) {
                throw new IllegalArgumentException("referenceRewrites must not contain null");
            }
            copy.add(referenceRewrite);
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

    public TagCategoryDraftState getDraftState() {
        return draftState;
    }

    public List<TagReferenceRewrite> getReferenceRewrites() {
        return referenceRewrites;
    }
}
