package org.freeplane.features.bookmarks.mindmapmode.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.awt.Point;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.MapBookmarks;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmarkDescriptor;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

public class BookmarkToolbarLayoutAndIndexingTest {
	private MockedStatic<ResourceController> resourceControllerMock;
	private MockedStatic<Compat> compatMock;
	private MockedStatic<TextUtils> textUtilsMock;
	private BookmarksController bookmarksController;
	private ModeController modeController;
	private MapModel map;
	private BookmarkToolbar toolbar;
	private BookmarksToolbarBuilder builder;

	@Before
	public void setUp() throws Exception {
		ResourceController resourceController = mock(ResourceController.class);
		when(resourceController.getIcon(anyString())).thenReturn(new ImageIcon());
		when(resourceController.getImageIcon(anyString())).thenReturn(new ImageIcon());
		when(resourceController.getProperty(anyString())).thenReturn(null);
		when(resourceController.getProperty("icons.groups")).thenReturn("user");
		when(resourceController.getProperty("icons.state")).thenReturn("testState");
		when(resourceController.getProperty(anyString(), anyString())).thenAnswer(invocation -> invocation.getArgument(1));
		when(resourceController.getArrayProperty(anyString(), anyString())).thenReturn(new String[0]);
		when(resourceController.isApplet()).thenReturn(true);
		when(resourceController.getResourceStream(anyString()))
				.thenAnswer(invocation -> BookmarkToolbarLayoutAndIndexingTest.class.getResourceAsStream(invocation.getArgument(0)));
		resourceControllerMock = mockStatic(ResourceController.class);
		resourceControllerMock.when(ResourceController::getResourceController).thenReturn(resourceController);

		compatMock = mockStatic(Compat.class);
		compatMock.when(Compat::isApplet).thenReturn(true);

		textUtilsMock = mockStatic(TextUtils.class);
		textUtilsMock.when(() -> TextUtils.getText(any(String.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		textUtilsMock.when(() -> TextUtils.getText(any(String.class), any(String.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		textUtilsMock.when(() -> TextUtils.getOptionalText(any(String.class), nullable(String.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		textUtilsMock.when(() -> TextUtils.capitalize(any(String.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		bookmarksController = mock(BookmarksController.class);
		modeController = mock(ModeController.class);
		map = mock(MapModel.class);
		toolbar = new BookmarkToolbar(bookmarksController, map, false);
		builder = new BookmarksToolbarBuilder(modeController, bookmarksController);
	}

	@After
	public void tearDown() {
		if (textUtilsMock != null) {
			textUtilsMock.close();
		}
		if (compatMock != null) {
			compatMock.close();
		}
		if (resourceControllerMock != null) {
			resourceControllerMock.close();
		}
	}

	@Test
	public void buildsToolbarWithLeadingFollowRootToggleAndTrailingAddRootButton() {
		NodeModel root = new NodeModel("root", map);
		NodeModel bookmarkOneNode = new NodeModel("bookmarkOne", map);
		NodeModel bookmarkTwoNode = new NodeModel("bookmarkTwo", map);
		root.insert(bookmarkOneNode);
		root.insert(bookmarkTwoNode);
		whenBookmarksAre(Arrays.asList(bookmark(bookmarkOneNode, "One"), bookmark(bookmarkTwoNode, "Two")));

		builder.updateBookmarksToolbar(toolbar, map, selection(root));

		assertThat(toolbar.getComponentCount()).isEqualTo(6);
		assertThat(toolbar.getComponent(0)).isInstanceOf(JToggleButton.class);
		assertThat(toolbar.getComponent(1)).isInstanceOf(JToolBar.Separator.class);
		assertThat(toolbar.getComponent(2)).isInstanceOf(BookmarkButton.class);
		assertThat(toolbar.getComponent(3)).isInstanceOf(BookmarkButton.class);
		assertThat(toolbar.getComponent(4)).isInstanceOf(JToolBar.Separator.class);
		assertThat(toolbar.getComponent(5)).isInstanceOf(JButton.class)
				.isNotInstanceOf(BookmarkButton.class)
				.isNotInstanceOf(JToggleButton.class);
		assertThat(((BookmarkButton) toolbar.getComponent(2)).getBookmarkListIndex()).isEqualTo(0);
		assertThat(((BookmarkButton) toolbar.getComponent(3)).getBookmarkListIndex()).isEqualTo(1);
		assertThat(toolbar.getEndDropAnchor()).isSameAs(toolbar.getComponent(4));
	}

	@Test
	public void collapsesEmptyVisibleBookmarkSectionToSingleSeparator() {
		NodeModel root = new NodeModel("root", map);
		NodeModel outsideBranch = new NodeModel("outside", map);
		NodeModel outsideBookmarkNode = new NodeModel("outsideBookmark", map);
		NodeModel selectionRoot = new NodeModel("selectionRoot", map);
		root.insert(outsideBranch);
		outsideBranch.insert(outsideBookmarkNode);
		root.insert(selectionRoot);
		whenBookmarksAre(Collections.singletonList(bookmark(outsideBookmarkNode, "Outside")));
		toolbar.setFollowsViewRootScope(true);

		builder.updateBookmarksToolbar(toolbar, map, selection(selectionRoot));

		assertThat(toolbar.getComponentCount()).isEqualTo(3);
		assertThat(toolbar.getComponent(0)).isInstanceOf(JToggleButton.class);
		assertThat(toolbar.getComponent(1)).isInstanceOf(JToolBar.Separator.class);
		assertThat(toolbar.getComponent(2)).isInstanceOf(JButton.class)
				.isNotInstanceOf(BookmarkButton.class)
				.isNotInstanceOf(JToggleButton.class);
		assertThat(toolbar.getBookmarkListSize()).isEqualTo(1);
		assertThat(toolbar.getEndDropAnchor()).isSameAs(toolbar.getComponent(2));
	}

	@Test
	public void keepsFullBookmarkListIndicesForVisibleScopedBookmarks() {
		NodeModel root = new NodeModel("root", map);
		NodeModel outsideOne = new NodeModel("outsideOne", map);
		NodeModel selectionRoot = new NodeModel("selectionRoot", map);
		NodeModel insideOne = new NodeModel("insideOne", map);
		NodeModel outsideTwo = new NodeModel("outsideTwo", map);
		NodeModel insideTwo = new NodeModel("insideTwo", map);
		root.insert(outsideOne);
		root.insert(selectionRoot);
		selectionRoot.insert(insideOne);
		root.insert(outsideTwo);
		selectionRoot.insert(insideTwo);
		whenBookmarksAre(Arrays.asList(
				bookmark(outsideOne, "Outside one"),
				bookmark(insideOne, "Inside one"),
				bookmark(outsideTwo, "Outside two"),
				bookmark(insideTwo, "Inside two")));
		toolbar.setFollowsViewRootScope(true);

		builder.updateBookmarksToolbar(toolbar, map, selection(selectionRoot));

		assertThat(((BookmarkButton) toolbar.getComponent(2)).getBookmarkListIndex()).isEqualTo(1);
		assertThat(((BookmarkButton) toolbar.getComponent(3)).getBookmarkListIndex()).isEqualTo(3);
	}

	@Test
	public void calculatesMoveIndexFromBookmarkListIndicesInsteadOfToolbarComponentOrder() {
		BookmarkButton[] buttons = prepareToolbarWithVisibleButtons(new int[]{100, 200}, 60, 1, 3);
		BookmarkIndexCalculator calculator = new BookmarkIndexCalculator(toolbar);

		int targetIndex = calculator.calculateBookmarkMoveIndex(3, buttons[0], new Point(0, 10));

		assertThat(targetIndex).isEqualTo(1);
		assertThat(buttons[1].getBookmarkListIndex()).isEqualTo(3);
	}

	@Test
	public void calculatesEndInsertionIndexAfterLastVisibleBookmark() {
		prepareToolbarWithVisibleButtons(new int[]{100, 200}, 60, 1, 3);
		toolbar.setBookmarkListSize(4);
		BookmarkIndexCalculator calculator = new BookmarkIndexCalculator(toolbar);

		BookmarkIndexCalculator.ToolbarDropPosition position = calculator.calculateToolbarDropPosition(new Point(400, 10));

		assertThat(position.type).isEqualTo(BookmarkIndexCalculator.ToolbarDropPosition.Type.AT_END);
		assertThat(position.getInsertionIndex()).isEqualTo(4);
		assertThat(position.getTargetButton()).isNull();
	}

	@Test
	public void usesFullBookmarkCountForEndInsertionWhenNoBookmarksAreVisible() {
		toolbar.removeAll();
		toolbar.add(new JToggleButton());
		toolbar.addSeparator();
		toolbar.add(new JButton());
		toolbar.setBookmarkListSize(4);
		BookmarkIndexCalculator calculator = new BookmarkIndexCalculator(toolbar);

		BookmarkIndexCalculator.ToolbarDropPosition position = calculator.calculateToolbarDropPosition(new Point(400, 10));

		assertThat(position.type).isEqualTo(BookmarkIndexCalculator.ToolbarDropPosition.Type.AT_END);
		assertThat(position.getInsertionIndex()).isEqualTo(4);
	}

	private void whenBookmarksAre(List<NodeBookmark> bookmarks) {
		MapBookmarks mapBookmarks = mock(MapBookmarks.class);
		when(mapBookmarks.getBookmarks()).thenReturn(bookmarks);
		when(bookmarksController.getBookmarks(map)).thenReturn(mapBookmarks);
	}

	private IMapSelection selection(NodeModel selectionRoot) {
		IMapSelection selection = mock(IMapSelection.class);
		when(selection.getSelectionRoot()).thenReturn(selectionRoot);
		return selection;
	}

	private NodeBookmark bookmark(NodeModel node, String name) {
		return new NodeBookmark(node, new NodeBookmarkDescriptor(name, false));
	}

	private BookmarkButton[] prepareToolbarWithVisibleButtons(int[] xPositions, int width, int... bookmarkListIndices) {
		toolbar.removeAll();
		toolbar.add(new JToggleButton());
		toolbar.addSeparator();
		BookmarkButton[] buttons = new BookmarkButton[bookmarkListIndices.length];
		for (int i = 0; i < bookmarkListIndices.length; i++) {
			int bookmarkListIndex = bookmarkListIndices[i];
			BookmarkButton button = new BookmarkButton(
					bookmark(new NodeModel("node" + bookmarkListIndex, map), "B" + bookmarkListIndex),
					null);
			button.setBookmarkListIndex(bookmarkListIndex);
			toolbar.add(button);
			button.setBounds(xPositions[i], 0, width, 20);
			buttons[i] = button;
		}
		toolbar.addSeparator();
		toolbar.add(new JButton());
		return buttons;
	}
}
