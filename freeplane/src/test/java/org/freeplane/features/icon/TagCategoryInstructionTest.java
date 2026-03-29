package org.freeplane.features.icon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class TagCategoryInstructionTest {
    @Test
    public void acceptsCategorizedTopLevelAddPath() {
        TagCategoryInstruction uut = TagCategoryInstruction.addTag(
            Collections.singletonList("Context"),
            TagTargetLocation.CATEGORIZED);

        assertThat(uut.getType()).isEqualTo(TagCategoryInstructionType.ADD_TAG);
        assertThat(uut.getPath()).containsExactly("Context");
        assertThat(uut.getTargetLocation()).isEqualTo(TagTargetLocation.CATEGORIZED);
    }

    @Test
    public void rejectsMultiSegmentUncategorizedAddPath() {
        assertThatThrownBy(() -> TagCategoryInstruction.addTag(
            Arrays.asList("Context", "Work"),
            TagTargetLocation.UNCATEGORIZED))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("exactly one segment");
    }

    @Test
    public void rejectsUncategorizedMoveWithParentPath() {
        assertThatThrownBy(() -> TagCategoryInstruction.moveTag(
            Arrays.asList("Context", "Work"),
            TagTargetLocation.UNCATEGORIZED,
            Collections.singletonList("Context"),
            null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("newParentPath");
    }

    @Test
    public void acceptsUncategorizedMoveWithEmptyParentPath() {
        TagCategoryInstruction uut = TagCategoryInstruction.moveTag(
            Arrays.asList("Context", "Work"),
            TagTargetLocation.UNCATEGORIZED,
            Collections.emptyList(),
            null);

        assertThat(uut.getNewParentPath()).isEmpty();
    }
}
