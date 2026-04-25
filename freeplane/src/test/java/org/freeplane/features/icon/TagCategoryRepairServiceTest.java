package org.freeplane.features.icon;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TagCategoryRepairServiceTest {
    @Test
    public void replacesBoundaryWhitespaceAndCollectsUniqueReplacements() {
        TagCategoryRepairService uut = new TagCategoryRepairService();

        String empty = uut.replaceBoundaryWhitespace("");
        String blank = uut.replaceBoundaryWhitespace("   ");
        String padded = uut.replaceBoundaryWhitespace(" alpha ");
        String existing = uut.replaceBoundaryWhitespace("_alpha_");
        String duplicate = uut.replaceBoundaryWhitespace(" alpha ");

        assertThat(empty).isEqualTo("_");
        assertThat(blank).isEqualTo("___");
        assertThat(padded).isEqualTo("_alpha_");
        assertThat(existing).isEqualTo("_alpha_");
        assertThat(duplicate).isEqualTo("_alpha_");

        TagCategoryRepairService.RepairResult repairResult = uut.repairResult();

        assertThat(repairResult.hasChanges()).isTrue();
        assertThat(repairResult.toMessage())
            .containsOnlyOnce("\"\" -> \"_\"")
            .containsOnlyOnce("\"   \" -> \"___\"")
            .containsOnlyOnce("\" alpha \" -> \"_alpha_\"");
    }

    @Test
    public void keepsInteriorWhitespaceUnchanged() {
        TagCategoryRepairService uut = new TagCategoryRepairService();

        String normalized = uut.replaceBoundaryWhitespace("a b");

        assertThat(normalized).isEqualTo("a b");
        assertThat(uut.repairResult().hasChanges()).isFalse();
    }
}
