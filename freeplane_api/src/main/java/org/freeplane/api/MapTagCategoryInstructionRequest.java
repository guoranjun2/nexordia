package org.freeplane.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Batch of explicit tag category edit operations.
 * @since 1.13.3
 */
public class MapTagCategoryInstructionRequest {
    private final String baseRevision;
    private final List<MapTagCategoryInstruction> instructions;

    /**
     * @param baseRevision revision returned by {@link MapTagCategoriesRO#read()}
     * @param instructions ordered edit operations to apply
     */
    public MapTagCategoryInstructionRequest(String baseRevision, List<MapTagCategoryInstruction> instructions) {
        if (baseRevision == null || baseRevision.trim().isEmpty()) {
            throw new IllegalArgumentException("baseRevision must not be blank");
        }
        if (instructions == null || instructions.isEmpty()) {
            throw new IllegalArgumentException("instructions must not be empty");
        }
        this.baseRevision = baseRevision;
        this.instructions = copyInstructions(instructions);
    }

    private List<MapTagCategoryInstruction> copyInstructions(List<MapTagCategoryInstruction> source) {
        ArrayList<MapTagCategoryInstruction> copiedInstructions = new ArrayList<>(source.size());
        for (MapTagCategoryInstruction instruction : source) {
            if (instruction == null) {
                throw new IllegalArgumentException("instructions must not contain null");
            }
            copiedInstructions.add(instruction);
        }
        return Collections.unmodifiableList(copiedInstructions);
    }

    /** Returns the revision the edits were prepared against. */
    public String getBaseRevision() {
        return baseRevision;
    }

    /** Returns the ordered edit operations to apply. */
    public List<MapTagCategoryInstruction> getInstructions() {
        return instructions;
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseRevision, instructions);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapTagCategoryInstructionRequest)) {
            return false;
        }
        MapTagCategoryInstructionRequest other = (MapTagCategoryInstructionRequest) obj;
        return Objects.equals(baseRevision, other.baseRevision)
            && Objects.equals(instructions, other.instructions);
    }
}
