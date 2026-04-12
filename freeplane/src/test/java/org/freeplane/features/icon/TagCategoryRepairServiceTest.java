package org.freeplane.features.icon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.features.map.MapModel;
import org.junit.Test;

public class TagCategoryRepairServiceTest {
    @Test
    public void replacesBoundaryWhitespaceInUncategorizedTags() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        TagReference blankReference = uut.createTagReference("   ");
        TagReference paddedReference = uut.createTagReference(" alpha ");
        TagReference existingReference = uut.createTagReference("_alpha_");

        TagCategoryRepairService uutRepairService = new TagCategoryRepairService();

        TagCategoryRepairService.RepairResult repairResult = uutRepairService.repair(uut);

        assertThat(repairResult.hasChanges()).isTrue();
        assertThat(uut.getUncategorizedTags())
            .extracting(Tag::getContent)
            .containsExactly("___", "_alpha_");
        assertThat(blankReference.getContent()).isEqualTo("___");
        assertThat(paddedReference.getContent()).isEqualTo("_alpha_");
        assertThat(existingReference.getContent()).isEqualTo("_alpha_");
    }

    @Test
    public void replacesBoundaryWhitespaceInCategorizedPaths() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        TagReference reference = uut.createTagReference(" Project :: :: State ");

        TagCategoryRepairService uutRepairService = new TagCategoryRepairService();

        TagCategoryRepairService.RepairResult repairResult = uutRepairService.repair(uut);

        assertThat(repairResult.hasChanges()).isTrue();
        TagAssertions.assertThatSerializedWithoutColors(uut).isEqualTo("_Project_\n"
            + " _\n"
            + "  _State_\n");
        assertThat(reference.getContent()).isEqualTo("_Project_::_::_State_");
        assertThat(TagCategoryStateBuilder.from(uut).getCategories())
            .extracting(TagCategoryNode::getQualifiedName)
            .containsExactly("_Project_");
    }

    @Test
    public void keepsLegacyBlankCategoryAsUnderscores() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        uut.load("ttag#a9d100ff\n"
            + "   #65bdffff\n"
            + "  kk#ceaf00ff\n");
        TagCategoryRepairService uutRepairService = new TagCategoryRepairService();

        TagCategoryRepairService.RepairResult repairResult = uutRepairService.repair(uut);

        assertThat(repairResult.hasChanges()).isTrue();
        TagAssertions.assertThatSerializedWithoutColors(uut).isEqualTo("ttag\n"
            + " __\n"
            + "  kk\n");
        assertThat(uut.getTagsAsListModel()).extracting(Tag::getContent)
            .containsExactly("ttag", "ttag::__", "ttag::__::kk");
    }

    @Test
    public void repairsLegacyWhitespaceTagsWhenMapLoadCompletes() {
        TagCategories tagCategories = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        tagCategories.createTagReference("   ");
        tagCategories.createTagReference(" Project :: :: State ");
        MapModel mapModel = mock(MapModel.class);
        IconRegistry iconRegistry = mock(IconRegistry.class);
        when(mapModel.getIconRegistry()).thenReturn(iconRegistry);
        when(iconRegistry.getTagCategories()).thenReturn(tagCategories);

        TagCategoryRepairService uutRepairService = new TagCategoryRepairService();

        TagCategoryRepairService.RepairResult repairResult = uutRepairService.repairLoadedMap(mapModel);

        assertThat(repairResult.hasChanges()).isTrue();
        assertThat(repairResult.toMessage()).contains("_Project_");
        TagAssertions.assertThatSerializedWithoutColors(tagCategories).isEqualTo("_Project_\n"
            + " _\n"
            + "  _State_\n");
        assertThat(tagCategories.getUncategorizedTags())
            .extracting(Tag::getContent)
            .containsExactly("___");
        assertThat(TagCategoryStateBuilder.from(tagCategories).getCategories())
            .extracting(TagCategoryNode::getQualifiedName)
            .containsExactly("_Project_");
    }
}
