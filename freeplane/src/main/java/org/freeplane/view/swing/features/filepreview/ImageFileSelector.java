package org.freeplane.view.swing.features.filepreview;

import java.io.File;

import javax.swing.filechooser.FileFilter;

import org.freeplane.api.swing.JFileChooser;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.image.ImageStorage;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.url.UrlManager;

class ImageFileSelector {
	private ImageFileSelector() {
	}

	static File chooseImageFile(final Controller controller, final ViewerController viewerController,
			final ImageStorage imageStorage) {
		final UrlManager urlManager = controller.getModeController().getExtension(UrlManager.class);
		final JFileChooser chooser = urlManager.getFileChooser();
		chooser.setAcceptAllFileFilterUsed(false);
		chooser.setFileFilter(new FileFilter() {
			@Override
			public boolean accept(final File file) {
				return file.isDirectory() || viewerController.getViewerFactory().accept(file.toURI())
				        || imageStorage.isSupportedImageFile(file);
			}

			@Override
			public String getDescription() {
				final String viewerDescription = viewerController.getViewerFactory().getDescription();
				return viewerDescription != null ? viewerDescription : TextUtils.getText("bitmaps");
			}
		});
		chooser.setAccessory(new ImagePreview(chooser));
		final int returnVal = chooser.showOpenDialog(controller.getViewController().getCurrentRootComponent());
		return returnVal == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
	}
}
