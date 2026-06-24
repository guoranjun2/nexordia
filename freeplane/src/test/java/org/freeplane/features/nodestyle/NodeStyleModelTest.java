package org.freeplane.features.nodestyle;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;

import org.junit.Test;

public class NodeStyleModelTest {
	@Test
	public void followThemeTextColorClearsFixedTextColor() {
		final NodeStyleModel model = new NodeStyleModel();

		model.setColor(Color.BLACK);
		model.setFollowThemeTextColor(Boolean.TRUE);

		assertThat(model.getColor()).isNull();
		assertThat(model.getFollowThemeTextColor()).isTrue();
	}

	@Test
	public void fixedTextColorClearsFollowThemeTextColor() {
		final NodeStyleModel model = new NodeStyleModel();

		model.setFollowThemeTextColor(Boolean.TRUE);
		model.setColor(Color.WHITE);

		assertThat(model.getColor()).isEqualTo(Color.WHITE);
		assertThat(model.getFollowThemeTextColor()).isNull();
	}

	@Test
	public void copyingFollowThemeTextColorClearsTargetFixedTextColor() {
		final NodeStyleModel source = new NodeStyleModel();
		final NodeStyleModel target = new NodeStyleModel();

		source.setFollowThemeTextColor(Boolean.TRUE);
		target.setColor(Color.BLACK);
		source.copyTo(target);

		assertThat(target.getColor()).isNull();
		assertThat(target.getFollowThemeTextColor()).isTrue();
	}

	@Test
	public void copyingFixedTextColorClearsTargetFollowThemeTextColor() {
		final NodeStyleModel source = new NodeStyleModel();
		final NodeStyleModel target = new NodeStyleModel();

		source.setColor(Color.WHITE);
		target.setFollowThemeTextColor(Boolean.TRUE);
		source.copyTo(target);

		assertThat(target.getColor()).isEqualTo(Color.WHITE);
		assertThat(target.getFollowThemeTextColor()).isNull();
	}
}
