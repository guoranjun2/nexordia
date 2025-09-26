package org.freeplane.view.swing.map.outline;

import org.junit.Test;
import static org.assertj.core.api.Assertions.assertThat;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class BreadcrumbPathTest {

    @Test
    public void computesBreadcrumbFromFirstFullyVisibleNode() {
        // Tree: root -> a -> b
        TreeNode root = new TreeNode("r", () -> "root");
        TreeNode a = new TreeNode("a", () -> "a");
        TreeNode b = new TreeNode("b", () -> "b");
        root.addChild(a);
        a.addChild(b);
        root.applyExpansionLevel(2);

        final OutlineGeometry geometry = OutlineGeometry.getInstance();
        VisibleOutlineState visibleState = new VisibleOutlineState(root);

        JPanel view = new JPanel();
        view.setPreferredSize(new Dimension(600, geometry.rowHeight * 100));
        JScrollPane scroll = new JScrollPane(view);
        JViewport jv = scroll.getViewport();
        jv.setView(view);
        jv.setExtentSize(new Dimension(600, geometry.rowHeight * 5));

        jv.setViewPosition(new Point(0, geometry.rowHeight * 2));

        BreadcrumbPath uut = new BreadcrumbPath(visibleState);

        BreadcrumbState state = uut.calculateBreadcrumbStateForIndex(2);
        assertThat(state).isNotNull();
        assertThat(state.getFirstVisibleNodeIndex()).isEqualTo(2);
        List<TreeNode> crumbs = state.getBreadcrumbNodes();
        assertThat(crumbs).hasSize(2);
        assertThat(crumbs.get(0)).isSameAs(root);
        assertThat(crumbs.get(1)).isSameAs(a);
        assertThat(state.getBreadcrumbHeight()).isEqualTo(geometry.rowHeight * 2);
    }
}

