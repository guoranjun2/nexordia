package org.freeplane.view.swing.map.outline;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Rectangle;

import org.junit.Test;

public class NodePositioningTest {

    @Test
    public void calculateTextButtonXIsMonotonic() {
        OutlineGeometry geometry = OutlineGeometry.getInstance();

        int x0 = geometry.calculateNodeButtonX(true, 0);
        int x1 = geometry.calculateNodeButtonX(true, 1);
        int x2 = geometry.calculateNodeButtonX(true, 2);

        assertThat(x0).isLessThanOrEqualTo(x1);
        assertThat(x1).isLessThanOrEqualTo(x2);
    }

    @Test
    public void calculateFirstVisibleNodeIndexRespectsBreadcrumbHeight() {
        OutlineGeometry geometry = OutlineGeometry.getInstance();
        VisibleOutlineNodes vs = new VisibleOutlineNodes(new TreeNode("r", () -> "root"));
        NodePositioning defaultPos = new NodePositioning(geometry, vs, 0);

        Rectangle viewRect = new Rectangle(0, 0, 400, geometry.rowHeight * 10);

        int idxNoBreadcrumb = defaultPos.calculateFirstVisibleNodeIndex(viewRect, 0);
        int idxWithBreadcrumb = defaultPos.calculateFirstVisibleNodeIndex(viewRect, geometry.rowHeight * 2);

        assertThat(idxNoBreadcrumb).isEqualTo(0);
        assertThat(idxWithBreadcrumb).isGreaterThanOrEqualTo(1);
        assertThat(idxWithBreadcrumb).isGreaterThanOrEqualTo(idxNoBreadcrumb);

        Rectangle offsetRect = new Rectangle(0, geometry.rowHeight, 400, geometry.rowHeight * 10);
        NodePositioning selectionDrivenPos = new NodePositioning(geometry, vs, geometry.rowHeight);
        int idxWithOffset = selectionDrivenPos.calculateFirstVisibleNodeIndex(offsetRect, 0);
        assertThat(idxWithOffset).isEqualTo(0);
    }
}
