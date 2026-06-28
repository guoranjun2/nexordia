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
		assertThat(FoldingControlGestureAction.isScaleGesture(event)).isFalse();
		assertThat(FoldingControlGestureAction.hasModifier(event)).isFalse();
	}

	@Test
	public void usesControlWheelForScaling() {
		MouseWheelEvent event = wheelEvent(InputEvent.CTRL_DOWN_MASK);

		assertThat(FoldingControlGestureAction.isScaleGesture(event)).isTrue();
	}

	@Test
	public void usesControlMagnificationForScaling() {
		assertThat(FoldingControlGestureAction.isScaleGesture(true, false, false, false)).isTrue();
		assertThat(FoldingControlGestureAction.hasMagnificationAction(true, false, false, false)).isTrue();
	}

	@Test
	public void amplifiesMagnificationDirection() {
		assertThat(FoldingControlGestureAction.directionForMagnification(0.25d)).isEqualTo(5d);
	}

	@Test
	public void doesNotScaleWithoutControl() {
		MouseWheelEvent event = wheelEvent(0);

		assertThat(FoldingControlGestureAction.isScaleGesture(event)).isFalse();
	}

	@Test
	public void doesNotScaleWithPhysicalShift() {
		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.KEY_PRESSED));
		MouseWheelEvent event = wheelEvent(InputEvent.CTRL_DOWN_MASK);

		assertThat(FoldingControlGestureAction.isScaleGesture(event)).isFalse();
	}

	@Test
	public void usesPhysicalShiftForRotation() {
		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.KEY_PRESSED));
		MouseWheelEvent event = wheelEvent(InputEvent.SHIFT_DOWN_MASK);

		assertThat(FoldingControlGestureAction.isRotationGesture(event)).isTrue();
		assertThat(FoldingControlGestureAction.hasModifier(event)).isTrue();
	}

	@Test
	public void usesShiftMagnificationForRotation() {
		assertThat(FoldingControlGestureAction.isRotationGesture(false, true, false, false)).isTrue();
		assertThat(FoldingControlGestureAction.hasMagnificationAction(false, true, false, false)).isTrue();
	}

	@Test
	public void ignoresCombinedControlAndShiftMagnification() {
		assertThat(FoldingControlGestureAction.hasMagnificationAction(true, true, false, false)).isFalse();
	}

	@Test
	public void tracksPhysicalControlState() {
		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.VK_CONTROL, KeyEvent.KEY_PRESSED));

		assertThat(PhysicalKeyboardModifierState.isControlPressed()).isTrue();

		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.VK_CONTROL, KeyEvent.KEY_RELEASED));

		assertThat(PhysicalKeyboardModifierState.isControlPressed()).isFalse();
	}

	@Test
	public void clearsPhysicalShiftOnRelease() {
		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.KEY_PRESSED));
		PhysicalKeyboardModifierState.updateFrom(keyEvent(KeyEvent.KEY_RELEASED));
		MouseWheelEvent event = wheelEvent(InputEvent.SHIFT_DOWN_MASK);

		assertThat(FoldingControlGestureAction.isRotationGesture(event)).isFalse();
	}

	private KeyEvent keyEvent(int id) {
		return keyEvent(KeyEvent.VK_SHIFT, id);
	}

	private KeyEvent keyEvent(int keyCode, int id) {
		return new KeyEvent(source, id, 0, id == KeyEvent.KEY_PRESSED ? modifierMask(keyCode) : 0,
				keyCode, KeyEvent.CHAR_UNDEFINED);
	}

	private int modifierMask(int keyCode) {
		if (keyCode == KeyEvent.VK_SHIFT) {
			return InputEvent.SHIFT_DOWN_MASK;
		}
		if (keyCode == KeyEvent.VK_CONTROL) {
			return InputEvent.CTRL_DOWN_MASK;
		}
		return 0;
	}

	private MouseWheelEvent wheelEvent(int modifiers) {
		return new MouseWheelEvent(source, MouseEvent.MOUSE_WHEEL, 0, modifiers, 0, 0, 0, 0, 0, false,
				MouseWheelEvent.WHEEL_UNIT_SCROLL, 1, 1, 1d);
	}
}
