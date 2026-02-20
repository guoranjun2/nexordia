package org.freeplane.plugin.ai.tools.tagcategories;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.freeplane.features.icon.TagCategoryEdit;
import org.freeplane.features.icon.TagCategoryEditBatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TagCategoryEditBatchPayload {
    private final String mapIdentifier;
    private final String expectedRevision;
    private final List<TagCategoryEditPayload> operations;

    @JsonCreator
    public TagCategoryEditBatchPayload(@JsonProperty("mapIdentifier") String mapIdentifier,
                                       @JsonProperty("expectedRevision") String expectedRevision,
                                       @JsonProperty("operations") List<TagCategoryEditPayload> operations) {
        this.mapIdentifier = mapIdentifier;
        this.expectedRevision = expectedRevision;
        this.operations = operations;
    }

    public static TagCategoryEditBatchPayload fromEditBatch(String mapIdentifier, TagCategoryEditBatch batch) {
        ArrayList<TagCategoryEditPayload> operationPayloads = new ArrayList<>();
        for (TagCategoryEdit operation : batch.getOperations()) {
            operationPayloads.add(TagCategoryEditPayload.fromEdit(operation));
        }
        return new TagCategoryEditBatchPayload(mapIdentifier, batch.getExpectedRevision(), operationPayloads);
    }

    public TagCategoryEditBatch toEditBatch() {
        ArrayList<TagCategoryEdit> editOperations = new ArrayList<>();
        if (operations != null) {
            for (TagCategoryEditPayload operation : operations) {
                editOperations.add(operation.toEdit());
            }
        }
        return new TagCategoryEditBatch(expectedRevision, editOperations);
    }

    public String getMapIdentifier() {
        return mapIdentifier;
    }

    public String getExpectedRevision() {
        return expectedRevision;
    }

    public List<TagCategoryEditPayload> getOperations() {
        return operations;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapIdentifier, expectedRevision, operations);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategoryEditBatchPayload)) {
            return false;
        }
        TagCategoryEditBatchPayload other = (TagCategoryEditBatchPayload) obj;
        return Objects.equals(mapIdentifier, other.mapIdentifier)
            && Objects.equals(expectedRevision, other.expectedRevision)
            && Objects.equals(operations, other.operations);
    }
}
