package org.freeplane.features.image;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodestyle.NodeSizeModel;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.view.swing.features.filepreview.ExternalResource;
import org.freeplane.view.swing.features.filepreview.ViewerController;

public class ImageNodeUpdater {
	private final ImageHtmlInserter htmlInserter = new ImageHtmlInserter();

	public void insertImageAtTop(final NodeModel node, final StoredImage image) {
		removeExternalImage(node);
		final String html = htmlInserter.insertImageAtTop(node.getText(), image);
		MTextController.getController().setNodeText(node, html);
		if (htmlInserter.isImageOnly(html)) {
			setNodeWidthToImage(node, image);
		}
	}

	public String createImageHtml(final StoredImage image) {
		return htmlInserter.createImageHtml(image);
	}

	public void fitNodeWidthToImage(final NodeModel node, final StoredImage image) {
		if (!image.hasKnownSize()) {
			return;
		}
		final Quantity<LengthUnit> width = LengthUnit.pixelsInPt(image.getWidth());
		NodeSizeModel.setNodeMinWidth(node, width);
		NodeSizeModel.setMaxNodeWidth(node, width);
	}

	private void setNodeWidthToImage(final NodeModel node, final StoredImage image) {
		if (!image.hasKnownSize()) {
			return;
		}
		final ModeController modeController = Controller.getCurrentModeController();
		if (modeController == null) {
			return;
		}
		final MNodeStyleController styleController =
				(MNodeStyleController) modeController.getExtension(NodeStyleController.class);
		if (styleController == null) {
			return;
		}
		final Quantity<LengthUnit> width = LengthUnit.pixelsInPt(image.getWidth());
		styleController.setMinNodeWidth(node, width);
		styleController.setMaxNodeWidth(node, width);
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
