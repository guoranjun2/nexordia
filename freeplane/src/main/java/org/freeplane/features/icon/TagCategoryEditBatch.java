package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagCategoryEditBatch {
    private final String expectedRevision;
    private final List<TagCategoryEdit> operations;

    public TagCategoryEditBatch(String expectedRevision, List<TagCategoryEdit> operations) {
        if (expectedRevision == null || expectedRevision.trim().isEmpty()) {
            throw new IllegalArgumentException("expectedRevision must not be blank");
        }
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException("operations must not be empty");
        }
        this.expectedRevision = expectedRevision;
        this.operations = copyOperations(operations);
    }

    private List<TagCategoryEdit> copyOperations(List<TagCategoryEdit> source) {
        ArrayList<TagCategoryEdit> copiedOperations = new ArrayList<>(source.size());
        for (TagCategoryEdit operation : source) {
            if (operation == null) {
                throw new IllegalArgumentException("operations must not contain null");
            }
            copiedOperations.add(operation);
        }
        return Collections.unmodifiableList(copiedOperations);
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

    public List<TagCategoryEdit> getOperations() {
        return operations;
    }

    @Override
    public int hashCode() {
        return Objects.hash(expectedRevision, operations);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategoryEditBatch)) {
            return false;
        }
        TagCategoryEditBatch other = (TagCategoryEditBatch) obj;
        return Objects.equals(expectedRevision, other.expectedRevision)
            && Objects.equals(operations, other.operations);
    }
}
