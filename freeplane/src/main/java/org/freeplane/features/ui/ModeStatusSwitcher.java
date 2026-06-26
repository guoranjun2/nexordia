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

import javax.swing.Icon;
import javax.swing.JButton;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.mode.ModeController;

class ModeStatusSwitcher extends JButton implements IFreeplanePropertyListener {
	private static final long serialVersionUID = 1L;
	private static final String LOOKANDFEEL_PROPERTY = "lookandfeel";
	private static final String EDIT_ICON_PATH = "/images/generic_edit.svg?useAccentColor=true";
	private static final String VIEW_ICON_PATH = "/images/eye.svg?useAccentColor=true";

	ModeStatusSwitcher() {
		setFocusable(false);
		setMargin(new Insets(1, 6, 1, 6));
		updateMode();
		ResourceController.getResourceController().addPropertyChangeListener(this);
		addActionListener(this::toggleMode);
	}

	private void toggleMode(ActionEvent event) {
		final ResourceController resourceController = ResourceController.getResourceController();
		resourceController.setProperty(ModeController.VIEW_MODE_PROPERTY, Boolean.toString(!isViewMode()));
	}

	@Override
	public void propertyChanged(String propertyName, String newValue, String oldValue) {
		if (ModeController.VIEW_MODE_PROPERTY.equals(propertyName) || LOOKANDFEEL_PROPERTY.equals(propertyName)) {
			updateMode();
		}
	}

	private void updateMode() {
		final boolean viewMode = isViewMode();
		setIcon(createIcon(viewMode));
		setToolTipText(TextUtils.getOptionalText("OptionPanel." + ModeController.VIEW_MODE_PROPERTY + "." + viewMode));
		repaint();
	}

	private boolean isViewMode() {
		return ResourceController.getResourceController().getBooleanProperty(ModeController.VIEW_MODE_PROPERTY);
	}

	private static Icon createIcon(boolean viewMode) {
		return ResourceController.getResourceController().getIcon(viewMode ? VIEW_ICON_PATH : EDIT_ICON_PATH);
	}
}
