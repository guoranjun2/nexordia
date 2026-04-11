package org.freeplane.features.icon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.features.map.MapModel;
import org.junit.Test;

public class TagCategoryRepairServiceTest {
    @Test
    public void trimsAndRemovesInvalidUncategorizedTags() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        TagReference blankReference = uut.createTagReference("   ");
        TagReference paddedReference = uut.createTagReference(" alpha ");
        TagReference existingReference = uut.createTagReference("alpha");

        TagCategoryRepairService uutRepairService = new TagCategoryRepairService();

        TagCategoryRepairService.RepairResult repairResult = uutRepairService.repair(uut);

        assertThat(repairResult.hasChanges()).isTrue();
        assertThat(uut.getUncategorizedTags())
            .extracting(Tag::getContent)
            .containsExactly("alpha");
        assertThat(blankReference.getContent()).isEqualTo(Tag.REMOVED_TAG.getContent());
        assertThat(paddedReference.getContent()).isEqualTo("alpha");
        assertThat(existingReference.getContent()).isEqualTo("alpha");
    }

    @Test
    public void trimsCategorizedPathsAndRemovesBlankSegments() {
        TagCategories uut = new TagCategories(
            new DefaultMutableTreeNode("tags"),
            new DefaultMutableTreeNode("uncategorized_tags"),
            "::");
        TagReference reference = uut.createTagReference(" Project :: :: State ");

        TagCategoryRepairService uutRepairService = new TagCategoryRepairService();

        TagCategoryRepairService.RepairResult repairResult = uutRepairService.repair(uut);

        assertThat(repairResult.hasChanges()).isTrue();
        TagAssertions.assertThatSerializedWithoutColors(uut).isEqualTo("Project\n"
            + " State\n");
        assertThat(reference.getContent()).isEqualTo("Project::State");
        assertThat(TagCategoryStateBuilder.from(uut).getCategories())
            .extracting(TagCategoryNode::getQualifiedName)
            .containsExactly("Project");
    }

    @Test
    public void repairsLegacyBlankCategoryWithoutTurningColorIntoTagName() {
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
            + " kk\n");
        assertThat(uut.getTagsAsListModel()).extracting(Tag::getContent)
            .containsExactly("ttag", "ttag::kk");
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
        assertThat(repairResult.toMessage()).contains("Project");
        TagAssertions.assertThatSerializedWithoutColors(tagCategories).isEqualTo("Project\n"
            + " State\n");
        assertThat(tagCategories.getUncategorizedTags()).isEmpty();
        assertThat(TagCategoryStateBuilder.from(tagCategories).getCategories())
            .extracting(TagCategoryNode::getQualifiedName)
            .containsExactly("Project");
    }
}
