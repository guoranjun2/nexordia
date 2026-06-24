/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.freeplane.features.ui;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.Compat;
import org.freeplane.core.util.TextUtils;
import org.freeplane.core.ui.flatlaf.FlatAtomOneDarkContrastIJTheme;
import org.freeplane.core.ui.flatlaf.FlatSolarizedLightIJTheme;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

class ThemeStatusSwitcher extends JButton {
	private static final long serialVersionUID = 1L;
	private static final String LOOKANDFEEL_PROPERTY = "lookandfeel";

	ThemeStatusSwitcher() {
		super(createIcon());
		setFocusable(false);
		setMargin(new Insets(1, 4, 1, 4));
		setToolTipText("Theme");
		addActionListener(this::showThemeMenu);
	}

	private static Icon createIcon() {
		return ResourceController.getResourceController().getIcon("/images/colorpicker.svg?useAccentColor=true");
	}

	private void showThemeMenu(ActionEvent event) {
		final JPopupMenu menu = new JPopupMenu();
		final ButtonGroup themeGroup = new ButtonGroup();
		final String currentLookAndFeel = ResourceController.getResourceController().getProperty(LOOKANDFEEL_PROPERTY);
		for (ThemeChoice themeChoice : createThemeChoices()) {
			final JRadioButtonMenuItem item = new JRadioButtonMenuItem(themeChoice.label);
			item.setSelected(themeChoice.lookAndFeel.equals(currentLookAndFeel));
			item.addActionListener(e -> ResourceController.getResourceController().setProperty(LOOKANDFEEL_PROPERTY,
			    themeChoice.lookAndFeel));
			themeGroup.add(item);
			menu.add(item);
		}
		menu.show(this, 0, getHeight());
	}

	private static List<ThemeChoice> createThemeChoices() {
		final ArrayList<ThemeChoice> choices = new ArrayList<ThemeChoice>();
		choices.add(new ThemeChoice(TextUtils.getOptionalText("OptionPanel.default"), "default"));
		if (Compat.isMacOsX()) {
			choices.add(new ThemeChoice("Flat macOS Light", FlatMacLightLaf.class.getName()));
			choices.add(new ThemeChoice("Flat macOS Dark", FlatMacDarkLaf.class.getName()));
		}
		choices.add(new ThemeChoice("Flat Light", FlatLightLaf.class.getName()));
		choices.add(new ThemeChoice("Flat IntelliJ", FlatIntelliJLaf.class.getName()));
		choices.add(new ThemeChoice("Flat Solarized Light", FlatSolarizedLightIJTheme.class.getName()));
		choices.add(new ThemeChoice("Flat Dark", FlatDarkLaf.class.getName()));
		choices.add(new ThemeChoice("Flat Darcula", FlatDarculaLaf.class.getName()));
		choices.add(new ThemeChoice("Atom One Dark Contrast (Material)", FlatAtomOneDarkContrastIJTheme.class.getName()));
		return choices;
	}

	private static class ThemeChoice {
		private final String label;
		private final String lookAndFeel;

		private ThemeChoice(String label, String lookAndFeel) {
			this.label = label;
			this.lookAndFeel = lookAndFeel;
		}
	}
}
