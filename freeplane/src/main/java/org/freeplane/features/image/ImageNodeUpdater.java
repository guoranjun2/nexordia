package org.freeplane.features.image;

import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.view.swing.features.filepreview.ExternalResource;
import org.freeplane.view.swing.features.filepreview.ViewerController;

public class ImageNodeUpdater {
	private final ImageHtmlInserter htmlInserter = new ImageHtmlInserter();

	public void insertImageAtTop(final NodeModel node, final StoredImage image) {
		removeExternalImage(node);
		MTextController.getController().setNodeText(node, htmlInserter.insertImageAtTop(node.getText(), image.getSource()));
	}

	private void removeExternalImage(final NodeModel node) {
		if (node.getExtension(ExternalResource.class) == null) {
			return;
		}
		final ModeController modeController = Controller.getCurrentModeController();
		if (modeController == null) {
			return;
		}
		final ViewerController viewerController = modeController.getExtension(ViewerController.class);
		if (viewerController != null) {
			viewerController.undoableDeactivateHook(node);
		}
	}
}
