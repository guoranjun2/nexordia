package org.freeplane.view.swing.map.outline;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;

import javax.swing.JButton;

import org.junit.Test;

public class NodePositioningTest {

    @Test
    public void calculateTextButtonXIsMonotonic() {
        OutlineGeometry geometry = new OutlineGeometry(new JButton("▶"));

        int x0 = geometry.calculateNodeButtonX(0);
        int x1 = geometry.calculateNodeButtonX(1);
        int x2 = geometry.calculateNodeButtonX(2);

        assertThat(x0).isLessThanOrEqualTo(x1);
        assertThat(x1).isLessThanOrEqualTo(x2);
    }

    @Test
    public void calculateFirstVisibleNodeIndexRespectsBreadcrumbHeight() {
        OutlineGeometry geometry = new OutlineGeometry(new JButton("▶"));
        VisibleOutlineState vs = new VisibleOutlineState(new TreeNode("r", () -> "root"));
        NodePositioning pos = new NodePositioning(geometry, vs);

        Rectangle viewRect = new Rectangle(0, 0, 400, geometry.rowHeight * 10);

        int idxNoBreadcrumb = pos.calculateFirstVisibleNodeIndex(viewRect, 0);
        int idxWithBreadcrumb = pos.calculateFirstVisibleNodeIndex(viewRect, geometry.rowHeight * 2);

        assertThat(idxNoBreadcrumb).isEqualTo(0);
        assertThat(idxWithBreadcrumb).isGreaterThanOrEqualTo(1);
        assertThat(idxWithBreadcrumb).isGreaterThanOrEqualTo(idxNoBreadcrumb);
    }
}
