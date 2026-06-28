/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2010 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is created by Stefan Ott in 2011.
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
package org.freeplane.view.swing.features.filepreview;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.freeplane.core.ui.AFreeplaneAction;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.image.ImageNodeUpdater;
import org.freeplane.features.image.ImageStorage;
import org.freeplane.features.image.StoredImage;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;

/**
 *
 * @author Stefan Ott
 *
 *This action adds an external image to a node
 */
public class AddExternalImageAction extends AFreeplaneAction {
	private static final long serialVersionUID = 1L;

	public AddExternalImageAction() {
		super("ExternalImageAddAction");
	}

	public void actionPerformed(final ActionEvent event) {
		final MapController mapController = Controller.getCurrentModeController().getMapController();
		final Collection<NodeModel> nodes = mapController.getSelectedNodes();
		Controller controller = Controller.getCurrentController();
		final ViewerController vc = controller.getModeController()
		    .getExtension(ViewerController.class);
		final NodeModel selectedNode = mapController.getSelectedNode();
		if (selectedNode == null)
			return;
		final ImageStorage imageStorage = ImageStorage.currentSettings();
		final File mapFile = selectedNode.getMap().getFile();
		if (imageStorage.requiresSavedMap() && mapFile == null) {
			UITools.errorMessage(TextUtils.getRawText("map_not_saved"));
			return;
		}
		final File imageFile = ImageFileSelector.chooseImageFile(controller, vc, imageStorage);
		if (imageFile == null)
			return;
		final StoredImage storedImage;
		try {
			storedImage = imageStorage.storeImageFile(imageFile, mapFile);
		}
		catch (final IOException e) {
			LogUtils.warn(e);
			return;
		}
		final ImageNodeUpdater updater = new ImageNodeUpdater();
		for (final NodeModel node : nodes) {
			updater.insertImageAtTop(node, storedImage);
		}
	}
}
