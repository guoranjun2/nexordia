package org.freeplane.features.styles;

import java.awt.Color;

public final class ThemeColorResolver {
	private static final int BLACK_THRESHOLD = 64;
	private static final int WHITE_THRESHOLD = 191;

	private ThemeColorResolver() {
	}

	public static Color resolveForeground(Color color, boolean darkTheme) {
		if(isNeutral(color)) {
			return darkTheme ? Color.WHITE : Color.BLACK;
		}
		return color;
	}

	public static Color resolveBackground(Color color, Color themeBackground) {
		if(color == null || isTransparent(color) || isNearlyBlack(color) || isNearlyWhite(color)) {
			return themeBackground;
		}
		return color;
	}

	static boolean isNearlyBlack(Color color) {
		return color != null
		        && color.getAlpha() == 255
		        && color.getRed() <= BLACK_THRESHOLD
		        && color.getGreen() <= BLACK_THRESHOLD
		        && color.getBlue() <= BLACK_THRESHOLD;
	}

	static boolean isNearlyWhite(Color color) {
		return color != null
		        && color.getAlpha() == 255
		        && color.getRed() >= WHITE_THRESHOLD
		        && color.getGreen() >= WHITE_THRESHOLD
		        && color.getBlue() >= WHITE_THRESHOLD;
	}

	private static boolean isTransparent(Color color) {
		return color != null && color.getAlpha() == 0;
	}

	private static boolean isNeutral(Color color) {
		return color == null || isNearlyBlack(color) || isNearlyWhite(color);
	}
}
