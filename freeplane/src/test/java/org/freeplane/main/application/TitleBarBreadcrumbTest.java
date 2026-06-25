package org.freeplane.main.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TitleBarBreadcrumbTest {
	@Test
	public void keepsFullNodePathWhenItFits() {
		assertThat(TitleBarBreadcrumb.visibleNodePathIndexes(11)).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
				10);
	}

	@Test
	public void abbreviatesMiddleNodesAndKeepsCurrentNode() {
		assertThat(TitleBarBreadcrumb.visibleNodePathIndexes(12)).containsExactly(0, -1, 2, 3, 4, 5, 6, 7, 8, 9,
				10, 11);
	}

	@Test
	public void handlesEmptyNodePath() {
		assertThat(TitleBarBreadcrumb.visibleNodePathIndexes(0)).isEmpty();
	}
}
