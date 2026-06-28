package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

public final class PhysicalKeyboardModifierState {
	private static volatile boolean shiftPressed;
	private static volatile boolean controlPressed;
	private static volatile boolean altPressed;
	private static volatile boolean metaPressed;

	static {
		KeyboardFocusManager.getCurrentKeyboardFocusManager()
				.addKeyEventDispatcher(PhysicalKeyboardModifierState::updateFrom);
	}

	public static void initialize() {
	}

	public static boolean isShiftPressed() {
		return shiftPressed;
	}

	public static boolean isControlPressed() {
		return controlPressed;
	}

	public static boolean isAltPressed() {
		return altPressed;
	}

	public static boolean isMetaPressed() {
		return metaPressed;
	}

	static boolean updateFrom(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.VK_SHIFT) {
			shiftPressed = isPressed(event, shiftPressed);
		}
		else if (event.getKeyCode() == KeyEvent.VK_CONTROL) {
			controlPressed = isPressed(event, controlPressed);
		}
		else if (event.getKeyCode() == KeyEvent.VK_ALT) {
			altPressed = isPressed(event, altPressed);
		}
		else if (event.getKeyCode() == KeyEvent.VK_META) {
			metaPressed = isPressed(event, metaPressed);
		}
		return false;
	}

	static void reset() {
		shiftPressed = false;
		controlPressed = false;
		altPressed = false;
		metaPressed = false;
	}

	private static boolean isPressed(KeyEvent event, boolean currentValue) {
		if (event.getID() == KeyEvent.KEY_PRESSED) {
			return true;
		}
		if (event.getID() == KeyEvent.KEY_RELEASED) {
			return false;
		}
		return currentValue;
	}

	private PhysicalKeyboardModifierState() {
	}
}
