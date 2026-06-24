package org.freeplane.features.styles;

import java.awt.Color;

import org.freeplane.features.nodestyle.NodeStyleModel;

class ThemeTextColorUpdater {
	private final boolean darkTheme;

	ThemeTextColorUpdater(boolean darkTheme) {
		this.darkTheme = darkTheme;
	}

	boolean update(NodeStyleModel styleModel, boolean defaultStyle) {
		if(Boolean.TRUE.equals(styleModel.getFollowThemeTextColor())) {
			styleModel.setFollowThemeTextColor(null);
			styleModel.setColor(themeTextColor());
			return true;
		}
		final Color color = styleModel.getColor();
		if(color == null) {
			if(defaultStyle && darkTheme) {
				styleModel.setColor(Color.WHITE);
				return true;
			}
			return false;
		}
		if(darkTheme && ThemeColorResolver.isNearlyBlack(color)) {
			styleModel.setColor(Color.WHITE);
			return true;
		}
		if(! darkTheme && ThemeColorResolver.isNearlyWhite(color)) {
			styleModel.setColor(Color.BLACK);
			return true;
		}
		return false;
	}

	private Color themeTextColor() {
		return darkTheme ? Color.WHITE : Color.BLACK;
	}
}
