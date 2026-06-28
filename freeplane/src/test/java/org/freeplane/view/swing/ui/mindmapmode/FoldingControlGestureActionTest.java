package org.freeplane.view.swing.ui.mindmapmode;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Canvas;
import java.awt.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.junit.After;
import org.junit.Test;

public class FoldingControlGestureActionTest {
	private final Component source = new Canvas();

	@After
	public void resetKeyboardState() {
		PhysicalKeyboardModifierState.reset();
	}

	@Test
	public void ignoresSyntheticShiftFromWheelEventForRotation() {
		MouseWheelEvent event = wheelEvent(InputEvent.SHIFT_DOWN_MASK);

		assertThat(FoldingControlGestureAction.isRotationGesture(event)).isFalse();
		assertThat(FoldingControlGestureAction.hasModifier(event)).isFalse();
	}

	@Test
	public void usesPhysicalShiftForRotation() {
		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.KEY_PRESSED));
		MouseWheelEvent event = wheelEvent(InputEvent.SHIFT_DOWN_MASK);

		assertThat(FoldingControlGestureAction.isRotationGesture(event)).isTrue();
		assertThat(FoldingControlGestureAction.hasModifier(event)).isTrue();
	}

	@Test
	public void clearsPhysicalShiftOnRelease() {
		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.KEY_PRESSED));
		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.KEY_RELEASED));
		MouseWheelEvent event = wheelEvent(InputEvent.SHIFT_DOWN_MASK);

		assertThat(FoldingControlGestureAction.isRotationGesture(event)).isFalse();
	}

	private KeyEvent keyEvent(int id) {
		return new KeyEvent(source, id, 0, id == KeyEvent.KEY_PRESSED ? InputEvent.SHIFT_DOWN_MASK : 0,
				KeyEvent.VK_SHIFT, KeyEvent.CHAR_UNDEFINED);
	}

	private MouseWheelEvent wheelEvent(int modifiers) {
		return new MouseWheelEvent(source, MouseEvent.MOUSE_WHEEL, 0, modifiers, 0, 0, 0, 0, 0, false,
				MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1, 1d);
	}
}
