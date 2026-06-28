package org.freeplane.features.image;

import java.awt.Image;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;

import org.freeplane.core.resources.ResourceController;

public class ImageStorage {
	private final File userDirectory;
	private final ImageStorageLocation location;

	public ImageStorage(final File userDirectory, final ImageStorageLocation location) {
		this.userDirectory = userDirectory;
		this.location = location;
	}

	public static ImageStorage currentSettings() {
		final String userDirectory = ResourceController.getResourceController().getFreeplaneUserDirectory();
		return new ImageStorage(userDirectory != null ? new File(userDirectory) : null, ImageStorageLocation.current());
	}

	public ImageStorageLocation getLocation() {
		return location;
	}

	public boolean requiresSavedMap() {
		return location.requiresSavedMap();
	}

	public boolean isSupportedImageFile(final File file) {
		return ImageContent.isSupportedImageFile(file);
	}

	public StoredImage storeClipboardImage(final Image image, final File mapFile) throws IOException {
		return store(ImageContent.fromImage(image), mapFile);
	}

	public StoredImage storeImageFile(final File file, final File mapFile) throws IOException {
		return store(ImageContent.fromFile(file), mapFile);
	}

	StoredImage store(final ImageContent imageContent, final File mapFile) throws IOException {
		if (location == ImageStorageLocation.EMBEDDED) {
			final String data = Base64.getEncoder().encodeToString(imageContent.getBytes());
			return new StoredImage("data:" + imageContent.getMimeType() + ";base64," + data, imageContent.getFileName(),
					imageContent.getWidth(), imageContent.getHeight());
		}
		final File imageFile = new File(directory(mapFile), imageContent.getFileName());
		writeIfMissing(imageFile, imageContent.getBytes());
		final String source = location == ImageStorageLocation.CURRENT
		        ? mapFile.getParentFile().toURI().relativize(imageFile.toURI()).toString()
		        : imageFile.toURI().toString();
		return new StoredImage(source, imageFile.getName(), imageContent.getWidth(), imageContent.getHeight());
	}

	private File directory(final File mapFile) throws IOException {
		if (location == ImageStorageLocation.CURRENT) {
			if (mapFile == null) {
				throw new IOException("Map must be saved before saving images next to it");
			}
			final String mapName = mapFile.getName();
			final int extensionStart = mapName.lastIndexOf('.');
			final String baseName = extensionStart >= 0 ? mapName.substring(0, extensionStart) : mapName;
			return new File(new File(mapFile.getParentFile(), baseName + "_files"), "img");
		}
		if (userDirectory == null) {
			throw new IOException("Freeplane user directory is not available");
		}
		return new File(userDirectory, "image");
	}

	private static void writeIfMissing(final File imageFile, final byte[] bytes) throws IOException {
		if (imageFile.isFile()) {
			return;
		}
		imageFile.getParentFile().mkdirs();
		final File tempFile = File.createTempFile(imageFile.getName(), ".tmp", imageFile.getParentFile());
		try {
			try (FileOutputStream outputStream = new FileOutputStream(tempFile)) {
				outputStream.write(bytes);
			}
			try {
				Files.move(tempFile.toPath(), imageFile.toPath(), StandardCopyOption.ATOMIC_MOVE);
			}
			catch (final AtomicMoveNotSupportedException e) {
				Files.move(tempFile.toPath(), imageFile.toPath());
			}
			catch (final FileAlreadyExistsException e) {
			}
		}
		finally {
			if (tempFile.exists()) {
				tempFile.delete();
			}
		}
	}
}
