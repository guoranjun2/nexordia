package org.freeplane.features.styles;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;

import org.freeplane.features.nodestyle.NodeStyleModel;
import org.junit.Test;

public class ThemeTextColorUpdaterTest {
	@Test
	public void darkThemeTurnsBlackTextWhite() {
		final NodeStyleModel model = new NodeStyleModel();
		model.setColor(Color.BLACK);

		final boolean changed = new ThemeTextColorUpdater(true).update(model, false);

		assertThat(changed).isTrue();
		assertThat(model.getColor()).isEqualTo(Color.WHITE);
	}

	@Test
	public void darkThemeKeepsWhiteTextWhite() {
		final NodeStyleModel model = new NodeStyleModel();
		model.setColor(Color.WHITE);

		final boolean changed = new ThemeTextColorUpdater(true).update(model, false);

		assertThat(changed).isFalse();
		assertThat(model.getColor()).isEqualTo(Color.WHITE);
	}

	@Test
	public void lightThemeTurnsWhiteTextBlack() {
		final NodeStyleModel model = new NodeStyleModel();
		model.setColor(Color.WHITE);

		final boolean changed = new ThemeTextColorUpdater(false).update(model, false);

		assertThat(changed).isTrue();
		assertThat(model.getColor()).isEqualTo(Color.BLACK);
	}

	@Test
	public void keepsNonBlackAndWhiteColors() {
		final NodeStyleModel model = new NodeStyleModel();
		model.setColor(Color.RED);

		final boolean changed = new ThemeTextColorUpdater(true).update(model, false);

		assertThat(changed).isFalse();
		assertThat(model.getColor()).isEqualTo(Color.RED);
	}

	@Test
	public void darkThemeTurnsNearlyBlackTextWhite() {
		final NodeStyleModel model = new NodeStyleModel();
		model.setColor(new Color(51, 51, 51));

		final boolean changed = new ThemeTextColorUpdater(true).update(model, false);

		assertThat(changed).isTrue();
		assertThat(model.getColor()).isEqualTo(Color.WHITE);
	}

	@Test
	public void lightThemeTurnsNearlyWhiteTextBlack() {
		final NodeStyleModel model = new NodeStyleModel();
		model.setColor(new Color(238, 238, 238));

		final boolean changed = new ThemeTextColorUpdater(false).update(model, false);

		assertThat(changed).isTrue();
		assertThat(model.getColor()).isEqualTo(Color.BLACK);
	}

	@Test
	public void keepsDarkSemanticColors() {
		final NodeStyleModel darkRed = new NodeStyleModel();
		final NodeStyleModel darkBlue = new NodeStyleModel();
		darkRed.setColor(new Color(85, 0, 0));
		darkBlue.setColor(new Color(0, 0, 128));

		final boolean changedRed = new ThemeTextColorUpdater(true).update(darkRed, false);
		final boolean changedBlue = new ThemeTextColorUpdater(true).update(darkBlue, false);

		assertThat(changedRed).isFalse();
		assertThat(changedBlue).isFalse();
		assertThat(darkRed.getColor()).isEqualTo(new Color(85, 0, 0));
		assertThat(darkBlue.getColor()).isEqualTo(new Color(0, 0, 128));
	}

	@Test
	public void keepsTransparentBlackText() {
		final NodeStyleModel model = new NodeStyleModel();
		model.setColor(new Color(0, 0, 0, 128));

		final boolean changed = new ThemeTextColorUpdater(true).update(model, false);

		assertThat(changed).isFalse();
		assertThat(model.getColor()).isEqualTo(new Color(0, 0, 0, 128));
	}

	@Test
	public void darkThemeGivesDefaultStyleWithoutColorWhiteText() {
		final NodeStyleModel model = new NodeStyleModel();

		final boolean changed = new ThemeTextColorUpdater(true).update(model, true);

		assertThat(changed).isTrue();
		assertThat(model.getColor()).isEqualTo(Color.WHITE);
	}

	@Test
	public void convertsLegacyFollowThemeTextColorToFixedThemeColor() {
		final NodeStyleModel model = new NodeStyleModel();
		model.setFollowThemeTextColor(Boolean.TRUE);

		final boolean changed = new ThemeTextColorUpdater(false).update(model, false);

		assertThat(changed).isTrue();
		assertThat(model.getColor()).isEqualTo(Color.BLACK);
		assertThat(model.getFollowThemeTextColor()).isNull();
	}
}
