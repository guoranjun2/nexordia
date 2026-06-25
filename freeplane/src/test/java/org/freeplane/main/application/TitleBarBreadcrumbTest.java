package org.freeplane.main.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TitleBarBreadcrumbTest {
	@Test
	public void keepsMoreNodesForShortNodeText() {
		assertThat(TitleBarBreadcrumb.visibleNodePathIndexes(pathTexts(12, 4))).containsExactly(0, -1, 2, 3, 4, 5,
				6, 7, 8, 9, 10, 11);
	}

	@Test
	public void keepsFewerNodesForLongNodeText() {
		assertThat(TitleBarBreadcrumb.visibleNodePathIndexes(pathTexts(8, 24))).containsExactly(0, -1, 5, 6, 7);
	}

	@Test
	public void keepsFullNodePathWhenNothingWouldBeOmitted() {
		assertThat(TitleBarBreadcrumb.visibleNodePathIndexes(pathTexts(4, 24))).containsExactly(0, 1, 2, 3);
	}

	@Test
	public void handlesEmptyNodePath() {
		assertThat(TitleBarBreadcrumb.visibleNodePathIndexes(new String[0])).isEmpty();
	}

	private String[] pathTexts(int count, int textLength) {
		String[] pathTexts = new String[count];
		for (int i = 0; i < count; i++) {
			pathTexts[i] = text(textLength);
		}
		return pathTexts;
	}

	private String text(int length) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < length; i++) {
			builder.append('x');
		}
		return builder.toString();
	}
}
