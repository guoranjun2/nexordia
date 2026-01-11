package org.freeplane.plugin.ai.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import javax.swing.tree.DefaultMutableTreeNode;

import org.freeplane.features.icon.IconRegistry;
import org.freeplane.features.icon.MindIcon;
import org.freeplane.features.icon.NamedIcon;
import org.freeplane.features.icon.TagCategories;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;

public class IconsContentEditorTest {
    @Test
    public void setInitialContent_addsIconByDescription() {
        MapModel mapModel = new MapModel(
            (source, targetMap, withChildren) -> null, iconRegistry(), null);
        NodeModel nodeModel = new NodeModel("node", mapModel);
        DefaultEnglishTextProvider englishTextProvider = new DefaultEnglishTextProvider();
        IconDescriptionResolver resolver = new IconDescriptionResolver(englishTextProvider);
        NamedIcon sampleIcon = new MindIcon("test", "/images/test.svg", "test", 0);
        IconsContentEditor unitUnderTest = new IconsContentEditor(resolver, Collections.singletonList(sampleIcon));
        IconsContent iconsContent = new IconsContent(Collections.singletonList(sampleIcon.getName()));

        unitUnderTest.setInitialContent(nodeModel, iconsContent);

        assertThat(nodeModel.getIcons()).hasSize(1);
        assertThat(nodeModel.getIcons().get(0)).isSameAs(sampleIcon);
    }

    private static IconRegistry iconRegistry() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("tags");
        DefaultMutableTreeNode uncategorized = new DefaultMutableTreeNode("uncategorized");
        return new IconRegistry(new TagCategories(root, uncategorized, "/"));
    }
}
