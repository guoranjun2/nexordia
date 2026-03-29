package org.freeplane.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class MapTagCategoryInstructionTest {
    @Test
    public void acceptsCategorizedTopLevelAddPath() {
        MapTagCategoryInstruction uut = new MapTagCategoryInstruction(
            MapTagCategoryInstructionType.ADD_TAG,
            Collections.singletonList("Context"),
            null,
            null,
            MapTagTargetLocation.CATEGORIZED,
            null,
            null,
            null);

        assertThat(uut.getType()).isEqualTo(MapTagCategoryInstructionType.ADD_TAG);
        assertThat(uut.getPath()).containsExactly("Context");
        assertThat(uut.getTargetLocation()).isEqualTo(MapTagTargetLocation.CATEGORIZED);
    }

    @Test
    public void rejectsMultiSegmentUncategorizedAddPath() {
        assertThatThrownBy(() -> new MapTagCategoryInstruction(
            MapTagCategoryInstructionType.ADD_TAG,
            Arrays.asList("Context", "Work"),
            null,
            null,
            MapTagTargetLocation.UNCATEGORIZED,
            null,
            null,
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactly one segment");
    }

    @Test
    public void acceptsUncategorizedMoveWithEmptyParentPath() {
        MapTagCategoryInstruction uut = new MapTagCategoryInstruction(
            MapTagCategoryInstructionType.MOVE_TAG,
            Arrays.asList("Context", "Work"),
            null,
            Collections.emptyList(),
            MapTagTargetLocation.UNCATEGORIZED,
            null,
            null,
            null);

        assertThat(uut.getNewParentPath()).isEmpty();
    }
}
