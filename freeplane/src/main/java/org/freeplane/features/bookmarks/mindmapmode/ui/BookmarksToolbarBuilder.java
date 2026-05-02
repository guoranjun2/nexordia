package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JToggleButton;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;
import org.freeplane.features.bookmarks.mindmapmode.BookmarkScope;
import org.freeplane.features.bookmarks.mindmapmode.BookmarksController;
import org.freeplane.features.bookmarks.mindmapmode.NodeBookmark;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;

public class BookmarksToolbarBuilder {
	private final BookmarksController bookmarksController;
	private final ModeController modeController;
	private final BookmarkButtonConfigurator buttonConfigurator;

	public BookmarksToolbarBuilder(ModeController modeController, BookmarksController bookmarksController) {
		this.modeController = modeController;
		this.bookmarksController = bookmarksController;
		this.buttonConfigurator = new BookmarkButtonConfigurator(bookmarksController, modeController);
	}

	public void updateBookmarksToolbar(BookmarkToolbar toolbar, MapModel map, IMapSelection selection) {
		final Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
		int focusIndex = -1;
		if(focusOwner != null && focusOwner.getParent() == toolbar) {
			focusIndex = toolbar.getComponentIndex(focusOwner);
		}

		toolbar.removeAll();

		List<NodeBookmark> bookmarks = bookmarksController.getBookmarks(map).getBookmarks();
		bookmarks = filterBookmarksBySelectionRoot(toolbar, map, selection, bookmarks);
		for (NodeBookmark bookmark : bookmarks) {
			final BookmarkButton button = createBookmarkButton(bookmark, toolbar, selection);
			toolbar.add(button);
			button.setFocusable(true);
		}

		final int bookmarkButtonCount = toolbar.getComponentCount();
		final JToggleButton followRootToggleButton = createFollowRootToggleButton(toolbar, map);

		JButton addRootBranchButton = TranslatedElementFactory.createButtonWithIcon("bookmark.addRootBranch.icon", "bookmark.addRootBranch.text");
		addRootBranchButton.addActionListener(e -> bookmarksController.addNewNode(map.getRootNode()));

		toolbar.addSeparator();
		toolbar.add(followRootToggleButton);
		toolbar.add(addRootBranchButton);
		addRootBranchButton.setFocusable(true);

		for(int i = 0; i < 3; i++) {
			final Component component = toolbar.getComponent(bookmarkButtonCount + i);
			buttonConfigurator.configureNonBookmarkComponent(component);
		}

		if(focusIndex >= 0) {
			if(toolbar.getComponentCount() > focusIndex) {
				Component component = toolbar.getComponent(focusIndex);
				if (component.isFocusable()) {
					component.requestFocusInWindow();
				}
			}
			else {
				toolbar.requestFocusInWindow();
			}
		}
		toolbar.revalidate();
		toolbar.repaint();
	}

	private BookmarkButton createBookmarkButton(NodeBookmark bookmark, BookmarkToolbar toolbar, IMapSelection selection) {
		final BookmarkButton button = new BookmarkButton(bookmark, modeController);
		buttonConfigurator.configureButton(button, bookmark, toolbar, selection);
		return button;
	}

	private JToggleButton createFollowRootToggleButton(BookmarkToolbar toolbar, MapModel map) {
		Icon icon = ResourceController.getResourceController().getIcon("/images/syncJumpIn.svg?useAccentColor=true");
		JToggleButton toggleButton = new JToggleButton(icon);
		toggleButton.setSelected(toolbar.followsViewRootScope());
		TranslatedElementFactory.createTooltip(toggleButton, "outline.followRoot");
		toggleButton.addActionListener(e -> {
			toolbar.setFollowsViewRootScope(toggleButton.isSelected());
			bookmarksController.updateBookmarksToolbar(toolbar, map);
		});
		return toggleButton;
	}

	private List<NodeBookmark> filterBookmarksBySelectionRoot(BookmarkToolbar toolbar, MapModel map,
			IMapSelection selection, List<NodeBookmark> bookmarks) {
		if (!toolbar.followsViewRootScope() || selection == null) {
			return bookmarks;
		}
		NodeModel selectionRoot = selection.getSelectionRoot();
		if (selectionRoot == null || selectionRoot.getMap() != map) {
			return bookmarks;
		}
		return bookmarks.stream()
				.filter(bookmark -> {
					NodeModel node = bookmark != null ? bookmark.getNode() : null;
					return BookmarkScope.isAtOrBelow(node, selectionRoot);
				})
				.collect(Collectors.toList());
	}

}
