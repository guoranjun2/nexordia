package org.freeplane.features.styles;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;

import org.junit.Test;

public class ThemeColorResolverTest {
	@Test
	public void darkThemeUsesWhiteForegroundForNeutralColors() {
		assertThat(ThemeColorResolver.resolveForeground(Color.BLACK, true)).isEqualTo(Color.WHITE);
		assertThat(ThemeColorResolver.resolveForeground(new Color(51, 51, 51), true)).isEqualTo(Color.WHITE);
	}

	@Test
	public void lightThemeUsesBlackForegroundForNeutralColors() {
		assertThat(ThemeColorResolver.resolveForeground(Color.WHITE, false)).isEqualTo(Color.BLACK);
		assertThat(ThemeColorResolver.resolveForeground(new Color(238, 238, 238), false)).isEqualTo(Color.BLACK);
	}

	@Test
	public void keepsSemanticForegroundColors() {
		Color color = new Color(85, 0, 0);

		assertThat(ThemeColorResolver.resolveForeground(color, true)).isEqualTo(color);
	}

	@Test
	public void keepsTransparentForegroundColors() {
		Color color = new Color(0, 0, 0, 128);

		assertThat(ThemeColorResolver.resolveForeground(color, true)).isEqualTo(color);
	}

	@Test
	public void usesThemeBackgroundForNeutralBackgrounds() {
		Color themeBackground = new Color(60, 63, 65);

		assertThat(ThemeColorResolver.resolveBackground(null, themeBackground)).isEqualTo(themeBackground);
		assertThat(ThemeColorResolver.resolveBackground(Color.BLACK, themeBackground)).isEqualTo(themeBackground);
		assertThat(ThemeColorResolver.resolveBackground(Color.WHITE, themeBackground)).isEqualTo(themeBackground);
	}

	@Test
	public void usesThemeBackgroundForTransparentBackgrounds() {
		Color themeBackground = new Color(60, 63, 65);

		assertThat(ThemeColorResolver.resolveBackground(new Color(0, 0, 0, 0), themeBackground)).isEqualTo(themeBackground);
	}

	@Test
	public void keepsSemanticBackgroundColors() {
		Color themeBackground = new Color(60, 63, 65);
		Color color = new Color(255, 240, 160);

		assertThat(ThemeColorResolver.resolveBackground(color, themeBackground)).isEqualTo(color);
	}
}
