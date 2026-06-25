package org.freeplane.features.ui.toolwindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class ToolWindowLayoutModelTest {
	@Test
	public void registersVisibleWindowAsDocked() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();

		ToolWindowState state = model.register("format", ToolWindowAnchor.RIGHT, true);

		assertThat(state.mode()).isEqualTo(ToolWindowMode.DOCKED);
		assertThat(state.anchor()).isEqualTo(ToolWindowAnchor.RIGHT);
		assertThat(state.region()).isEqualTo(ToolWindowLayoutModel.DEFAULT_REGION);
		assertThat(state.isVisible()).isTrue();
	}

	@Test
	public void registersHiddenWindowAsHidden() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();

		ToolWindowState state = model.register("icons", ToolWindowAnchor.LEFT, false);

		assertThat(state.mode()).isEqualTo(ToolWindowMode.HIDDEN);
		assertThat(state.isVisible()).isFalse();
	}

	@Test
	public void registersWindowWithRestoredMode() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();

		ToolWindowState state = model.register("note", ToolWindowAnchor.RIGHT, ToolWindowMode.FLOATING);

		assertThat(state.mode()).isEqualTo(ToolWindowMode.FLOATING);
		assertThat(state.restoreAnchor()).isEqualTo(ToolWindowAnchor.RIGHT);
	}

	@Test
	public void activatesHiddenWindowAtOriginalAnchor() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("icons", ToolWindowAnchor.LEFT, false);

		ToolWindowState state = model.activate("icons");

		assertThat(state.mode()).isEqualTo(ToolWindowMode.DOCKED);
		assertThat(state.anchor()).isEqualTo(ToolWindowAnchor.LEFT);
	}

	@Test
	public void deactivatesDockedWindowOnActivation() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("format", ToolWindowAnchor.RIGHT, true);

		ToolWindowState state = model.activate("format");

		assertThat(state.mode()).isEqualTo(ToolWindowMode.INACTIVE);
	}

	@Test
	public void keepsFloatingWindowFloatingOnActivation() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("format", ToolWindowAnchor.RIGHT, true);
		model.floatWindow("format");

		ToolWindowState state = model.activate("format");

		assertThat(state.mode()).isEqualTo(ToolWindowMode.FLOATING);
		assertThat(state.restoreAnchor()).isEqualTo(ToolWindowAnchor.RIGHT);
	}

	@Test
	public void docksFloatingWindowAtRestoreAnchor() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("format", ToolWindowAnchor.RIGHT, true);
		model.floatWindow("format");

		ToolWindowState state = model.dock("format");

		assertThat(state.mode()).isEqualTo(ToolWindowMode.DOCKED);
		assertThat(state.anchor()).isEqualTo(ToolWindowAnchor.RIGHT);
	}

	@Test
	public void docksWindowAtNewAnchor() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("format", ToolWindowAnchor.RIGHT, true);

		ToolWindowState state = model.dock("format", ToolWindowAnchor.LEFT);

		assertThat(state.mode()).isEqualTo(ToolWindowMode.DOCKED);
		assertThat(state.anchor()).isEqualTo(ToolWindowAnchor.LEFT);
		assertThat(state.restoreAnchor()).isEqualTo(ToolWindowAnchor.LEFT);
	}

	@Test
	public void deactivatesOtherDockedWindowInSameRegion() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("format", ToolWindowAnchor.RIGHT, ToolWindowMode.DOCKED, 0);
		model.register("note", ToolWindowAnchor.RIGHT, ToolWindowMode.HIDDEN, 0);

		ToolWindowState state = model.show("note");

		assertThat(state.mode()).isEqualTo(ToolWindowMode.DOCKED);
		assertThat(model.state("format").mode()).isEqualTo(ToolWindowMode.INACTIVE);
	}

	@Test
	public void keepsDockedWindowsVisibleInDifferentRegions() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();

		model.register("format", ToolWindowAnchor.RIGHT, ToolWindowMode.DOCKED, 0);
		model.register("note", ToolWindowAnchor.RIGHT, ToolWindowMode.DOCKED, 1);

		assertThat(model.state("format").mode()).isEqualTo(ToolWindowMode.DOCKED);
		assertThat(model.state("note").mode()).isEqualTo(ToolWindowMode.DOCKED);
	}

	@Test
	public void movingWindowToRegionDeactivatesOnlyThatRegionConflict() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("format", ToolWindowAnchor.RIGHT, ToolWindowMode.DOCKED, 0);
		model.register("outline", ToolWindowAnchor.LEFT, ToolWindowMode.DOCKED, 1);
		model.register("note", ToolWindowAnchor.RIGHT, ToolWindowMode.HIDDEN, 0);

		ToolWindowState state = model.moveToRegion("note", 0);

		assertThat(state.region()).isEqualTo(0);
		assertThat(state.mode()).isEqualTo(ToolWindowMode.DOCKED);
		assertThat(model.state("format").mode()).isEqualTo(ToolWindowMode.INACTIVE);
		assertThat(model.state("outline").mode()).isEqualTo(ToolWindowMode.DOCKED);
	}

	@Test
	public void showingHiddenWindowDoesNotCollectCurrentWindow() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("format", ToolWindowAnchor.RIGHT, ToolWindowMode.DOCKED, 0);
		model.register("note", ToolWindowAnchor.RIGHT, ToolWindowMode.HIDDEN, 0);

		model.show("note");

		assertThat(model.state("note").mode()).isEqualTo(ToolWindowMode.DOCKED);
		assertThat(model.state("format").mode()).isEqualTo(ToolWindowMode.INACTIVE);
	}

	@Test
	public void movesHiddenWindowToOpenRegionWithoutActivatingIt() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();
		model.register("format", ToolWindowAnchor.RIGHT, ToolWindowMode.DOCKED, 0);
		model.register("note", ToolWindowAnchor.RIGHT, ToolWindowMode.HIDDEN, 0);

		ToolWindowState state = model.moveHiddenToRegion("note", 1);

		assertThat(state.region()).isEqualTo(1);
		assertThat(state.mode()).isEqualTo(ToolWindowMode.HIDDEN);
		assertThat(model.state("format").mode()).isEqualTo(ToolWindowMode.DOCKED);
	}

	@Test
	public void rejectsUnknownWindowStateChanges() {
		ToolWindowLayoutModel model = new ToolWindowLayoutModel();

		assertThatThrownBy(() -> model.activate("missing"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("missing");
	}
}
