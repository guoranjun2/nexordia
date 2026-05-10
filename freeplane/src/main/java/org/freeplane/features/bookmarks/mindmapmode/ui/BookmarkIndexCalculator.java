package org.freeplane.features.bookmarks.mindmapmode.ui;

import java.awt.Component;
import java.awt.Point;

import javax.swing.JButton;

class BookmarkIndexCalculator {
	private final BookmarkToolbar toolbar;

	BookmarkIndexCalculator(BookmarkToolbar toolbar) {
		this.toolbar = toolbar;
	}

	int calculateBookmarkMoveIndex(int sourceIndex, JButton targetButton, Point dropPoint) {
		if (!(targetButton instanceof BookmarkButton)) {
			return -1;
		}
		int targetIndex = ((BookmarkButton) targetButton).getBookmarkListIndex();
		if (targetIndex < 0) {
			return -1;
		}
		boolean movesAfter = isDropAfter(targetButton, dropPoint);

		return movesAfter ? (sourceIndex < targetIndex ? targetIndex : targetIndex + 1)
		        : (sourceIndex < targetIndex ? targetIndex - 1 : targetIndex);
	}

	boolean isValidBookmarkMove(int sourceIndex, JButton targetButton, Point dropPoint) {
		if (!(targetButton instanceof BookmarkButton)) {
			return false;
		}
		Component parent = targetButton.getParent();
		if (!(parent instanceof BookmarkToolbar) || parent != toolbar) {
			return false;
		}

		int targetIndex = ((BookmarkButton) targetButton).getBookmarkListIndex();
		if (targetIndex < 0 || targetIndex == sourceIndex) {
			return false;
		}
		if (!isInInsertionZone(targetButton, dropPoint)) {
			return false;
		}

		int finalTargetIndex = calculateBookmarkMoveIndex(sourceIndex, targetButton, dropPoint);
		return finalTargetIndex >= 0 && sourceIndex != finalTargetIndex;
	}

	boolean isInInsertionZone(JButton targetButton, Point dropPoint) {
		int buttonWidth = targetButton.getWidth();
		int leftThird = buttonWidth / 3;
		int rightThird = buttonWidth * 2 / 3;
		return dropPoint.x <= leftThird || dropPoint.x >= rightThird;
	}

	boolean isDropAfter(JButton targetButton, Point dropPoint) {
		int rightThird = targetButton.getWidth() * 2 / 3;
		return dropPoint.x >= rightThird;
	}

	ToolbarDropPosition calculateToolbarDropPosition(Point dropPoint) {
		Component[] components = toolbar.getComponents();
		for (Component component : components) {
			if (component instanceof BookmarkButton) {
				BookmarkButton button = (BookmarkButton) component;
				int buttonLeft = button.getX();
				if (dropPoint.x >= buttonLeft - BookmarkToolbar.GAP && dropPoint.x <= buttonLeft + BookmarkToolbar.GAP) {
					return ToolbarDropPosition.before(button);
				}
			}
		}
		for (Component component : components) {
			if (component instanceof BookmarkButton) {
				BookmarkButton button = (BookmarkButton) component;
				int buttonRight = button.getX() + button.getWidth();
				if (dropPoint.x >= buttonRight - BookmarkToolbar.GAP && dropPoint.x <= buttonRight + BookmarkToolbar.GAP) {
					return ToolbarDropPosition.after(button);
				}
			}
		}
		return ToolbarDropPosition.atEnd(calculateEndInsertionIndex());
	}

	private int calculateEndInsertionIndex() {
		BookmarkButton lastBookmarkButton = null;
		for (Component component : toolbar.getComponents()) {
			if (component instanceof BookmarkButton) {
				lastBookmarkButton = (BookmarkButton) component;
			}
		}
		return lastBookmarkButton != null ? lastBookmarkButton.getBookmarkListIndex() + 1 : toolbar.getBookmarkListSize();
	}

	static class ToolbarDropPosition {
		enum Type { BEFORE_BUTTON, AFTER_BUTTON, AT_END }

		final Type type;
		private final BookmarkButton targetButton;
		private final int insertionIndex;

		private ToolbarDropPosition(Type type, BookmarkButton targetButton, int insertionIndex) {
			this.type = type;
			this.targetButton = targetButton;
			this.insertionIndex = insertionIndex;
		}

		static ToolbarDropPosition before(BookmarkButton targetButton) {
			return new ToolbarDropPosition(Type.BEFORE_BUTTON, targetButton, targetButton.getBookmarkListIndex());
		}

		static ToolbarDropPosition after(BookmarkButton targetButton) {
			return new ToolbarDropPosition(Type.AFTER_BUTTON, targetButton, targetButton.getBookmarkListIndex() + 1);
		}

		static ToolbarDropPosition atEnd(int insertionIndex) {
			return new ToolbarDropPosition(Type.AT_END, null, insertionIndex);
		}

		BookmarkButton getTargetButton() {
			return targetButton;
		}

		int getInsertionIndex() {
			return insertionIndex;
		}
	}
}
