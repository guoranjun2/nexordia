package org.freeplane.features.icon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class TagCategoryInstructionRequest {
    private final String baseRevision;
    private final List<TagCategoryInstruction> instructions;

    public TagCategoryInstructionRequest(String baseRevision, List<TagCategoryInstruction> instructions) {
        if (baseRevision == null || baseRevision.trim().isEmpty()) {
            throw new IllegalArgumentException("baseRevision must not be blank");
        }
        if (instructions == null || instructions.isEmpty()) {
            throw new IllegalArgumentException("instructions must not be empty");
        }
        this.baseRevision = baseRevision;
        this.instructions = copyInstructions(instructions);
    }

    private List<TagCategoryInstruction> copyInstructions(List<TagCategoryInstruction> source) {
        ArrayList<TagCategoryInstruction> copiedInstructions = new ArrayList<>(source.size());
        for (TagCategoryInstruction instruction : source) {
            if (instruction == null) {
                throw new IllegalArgumentException("instructions must not contain null");
            }
            copiedInstructions.add(instruction);
        }
        return Collections.unmodifiableList(copiedInstructions);
    }

    public void requireMatchingRevision(String currentRevision) {
        if (!baseRevision.equals(currentRevision)) {
            throw new TagCategoryConflictException(
                "stale revision: expected " + baseRevision + ", current " + currentRevision);
        }
    }

    public String getBaseRevision() {
        return baseRevision;
    }

    public List<TagCategoryInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseRevision, instructions);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TagCategoryInstructionRequest)) {
            return false;
        }
        TagCategoryInstructionRequest other = (TagCategoryInstructionRequest) obj;
        return Objects.equals(baseRevision, other.baseRevision)
            && Objects.equals(instructions, other.instructions);
    }
}
