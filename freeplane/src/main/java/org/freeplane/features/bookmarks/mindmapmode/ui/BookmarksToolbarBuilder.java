package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.List;

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

		List<NodeBookmark> allBookmarks = bookmarksController.getBookmarks(map).getBookmarks();
		toolbar.setBookmarkListSize(allBookmarks.size());

		final JToggleButton followRootToggleButton = createFollowRootToggleButton(toolbar, map);
		followRootToggleButton.setFocusable(true);
		toolbar.add(followRootToggleButton);
		buttonConfigurator.configureNonBookmarkComponent(followRootToggleButton, toolbar.isInteractive());

		toolbar.addSeparator();
		buttonConfigurator.configureNonBookmarkComponent(toolbar.getComponent(toolbar.getComponentCount() - 1), toolbar.isInteractive());

		int visibleBookmarkCount = 0;
		for (int bookmarkListIndex = 0; bookmarkListIndex < allBookmarks.size(); bookmarkListIndex++) {
			NodeBookmark bookmark = allBookmarks.get(bookmarkListIndex);
			if (!isVisibleInSelectionRoot(toolbar, map, selection, bookmark)) {
				continue;
			}
			final BookmarkButton button = createBookmarkButton(bookmark, bookmarkListIndex, toolbar, selection);
			toolbar.add(button);
			button.setFocusable(true);
			visibleBookmarkCount++;
		}

		if (visibleBookmarkCount > 0) {
			toolbar.addSeparator();
			buttonConfigurator.configureNonBookmarkComponent(toolbar.getComponent(toolbar.getComponentCount() - 1), toolbar.isInteractive());
		}

		JButton addRootBranchButton = TranslatedElementFactory.createButtonWithIcon("bookmark.addRootBranch.icon", "bookmark.addRootBranch.text");
		addRootBranchButton.addActionListener(e -> bookmarksController.addNewNode(map.getRootNode()));
		addRootBranchButton.setFocusable(true);
		toolbar.add(addRootBranchButton);
		buttonConfigurator.configureNonBookmarkComponent(addRootBranchButton, toolbar.isInteractive());

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

	private BookmarkButton createBookmarkButton(NodeBookmark bookmark, int bookmarkListIndex,
			BookmarkToolbar toolbar, IMapSelection selection) {
		final BookmarkButton button = new BookmarkButton(bookmark, modeController);
		button.setBookmarkListIndex(bookmarkListIndex);
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

	private boolean isVisibleInSelectionRoot(BookmarkToolbar toolbar, MapModel map,
			IMapSelection selection, NodeBookmark bookmark) {
		if (!toolbar.followsViewRootScope() || selection == null) {
			return true;
		}
		NodeModel selectionRoot = selection.getSelectionRoot();
		if (selectionRoot == null || selectionRoot.getMap() != map) {
			return true;
		}
		NodeModel node = bookmark != null ? bookmark.getNode() : null;
		return BookmarkScope.isAtOrBelow(node, selectionRoot);
	}

}
