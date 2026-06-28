package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

final class PhysicalKeyboardModifierState {
	private static volatile boolean shiftPressed;

	static {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.addKeyEventDispatcher(PhysicalKeyboardModifierState::updateFrom);
	}

	static void initialize() {
	}

	static boolean isShiftPressed() {
		return shiftPressed;
	}

	static boolean updateFrom(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_SHIFT) {
			if (event.getID() == KeyEvent.KEY_PRESSED) {
				shiftPressed = true;
			}
			else if (event.getID() == KeyEvent.KEY_RELEASED) {
				shiftPressed = false;
			}
		}
		return false;
	}

	static void reset() {
		shiftPressed = false;
	}

	private PhysicalKeyboardModifierState() {
	}
}
