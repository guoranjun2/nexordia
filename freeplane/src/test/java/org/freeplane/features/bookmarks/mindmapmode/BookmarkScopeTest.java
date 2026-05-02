package org.freeplane.features.bookmarks.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;
import org.freeplane.features.map.NodeModel;
import org.junit.Test;

public class BookmarkScopeTest {

    @Test
    public void returnsTrueForScopeRootItself() {
        NodeModel scopeRoot = new NodeModel("scopeRoot", null);

        assertThat(BookmarkScope.isAtOrBelow(scopeRoot, scopeRoot)).isTrue();
    }

    @Test
    public void returnsTrueForDescendantNode() {
        NodeModel scopeRoot = new NodeModel("scopeRoot", null);
        NodeModel child = new NodeModel("child", null);
        scopeRoot.insert(child, -1);

        assertThat(BookmarkScope.isAtOrBelow(child, scopeRoot)).isTrue();
    }

    @Test
    public void returnsFalseForNodeOutsideScope() {
        NodeModel scopeRoot = new NodeModel("scopeRoot", null);
        NodeModel otherRoot = new NodeModel("otherRoot", null);
        NodeModel otherChild = new NodeModel("otherChild", null);
        otherRoot.insert(otherChild, -1);

        assertThat(BookmarkScope.isAtOrBelow(otherChild, scopeRoot)).isFalse();
    }

    @Test
    public void returnsFalseWhenNodeOrScopeRootIsNull() {
        NodeModel scopeRoot = new NodeModel("scopeRoot", null);
        NodeModel node = new NodeModel("node", null);

        assertThat(BookmarkScope.isAtOrBelow(null, scopeRoot)).isFalse();
        assertThat(BookmarkScope.isAtOrBelow(node, null)).isFalse();
    }
}
