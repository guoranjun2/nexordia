package org.freeplane.features.icon.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.features.icon.Tag;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.icon.TagCategoriesTest;
import org.junit.Test;

public class MIconControllerTest {
    @Test
    public void normalizeTagsForPersistenceTrimsAndDropsBlankTags() {
        assertThat(MIconController.normalizeTagsForPersistence(Arrays.asList(
            new Tag("  "),
            new Tag(" alpha "),
            new Tag("beta"))))
            .extracting(Tag::getContent)
            .containsExactly("alpha", "beta");
    }

    @Test
    public void submenuActionTagUsesQualifiedCategorizedTag() {
        TagCategories tagCategories = TagCategoriesTest.tagCategories("ttag\n"
            + " kk\n");
        DefaultMutableTreeNode topLevelNode = (DefaultMutableTreeNode) tagCategories.getRootNode().getChildAt(0);
        DefaultMutableTreeNode childNode = (DefaultMutableTreeNode) topLevelNode.getChildAt(0);

        Tag actionTag = MIconController.submenuActionTag(tagCategories, childNode);

        assertThat(actionTag.getContent()).isEqualTo("ttag::kk");
    }
}
