package org.freeplane.features.image;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.ImageIcon;

import org.freeplane.core.util.FileUtils;

class ImageContent {
	private static final String PNG_EXTENSION = "png";
	private static final String PNG_MIME_TYPE = "image/png";

	private final byte[] bytes;
	private final String extension;
	private final String mimeType;
	private final String hash;
	private final int width;
	private final int height;

	private ImageContent(final byte[] bytes, final String extension, final String mimeType, final int width,
			final int height) {
		this.bytes = bytes;
		this.extension = extension;
		this.mimeType = mimeType;
		this.hash = sha256(bytes);
		this.width = width;
		this.height = height;
	}

	static ImageContent fromImage(final Image image) throws IOException {
		final BufferedImage bufferedImage = toBufferedImage(image);
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, PNG_EXTENSION, outputStream);
		return new ImageContent(outputStream.toByteArray(), PNG_EXTENSION, PNG_MIME_TYPE, bufferedImage.getWidth(),
				bufferedImage.getHeight());
	}

	static ImageContent fromFile(final File file) throws IOException {
		final String extension = FileUtils.getExtension(file);
		final Dimension size = imageSize(file, extension);
		return new ImageContent(Files.readAllBytes(file.toPath()), extension, mimeType(file, extension),
				size != null ? size.width : -1, size != null ? size.height : -1);
	}

	static boolean isSupportedImageFile(final File file) {
		if (file == null || !file.isFile() || !file.canRead()) {
			return false;
		}
		final String extension = FileUtils.getExtension(file);
		if ("svg".equals(extension)) {
			return true;
		}
		final Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix(extension);
		return readers.hasNext();
	}

	byte[] getBytes() {
		return bytes;
	}

	String getFileName() {
		return "img-" + hash + "." + extension;
	}

	String getMimeType() {
		return mimeType;
	}

	int getWidth() {
		return width;
	}

	int getHeight() {
		return height;
	}

	private static BufferedImage toBufferedImage(final Image image) throws IOException {
		final ImageIcon icon = new ImageIcon(image);
		final int width = icon.getIconWidth();
		final int height = icon.getIconHeight();
		if (width <= 0 || height <= 0) {
			throw new IOException("Can not read clipboard image size");
		}
		final BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = bufferedImage.createGraphics();
		graphics.drawImage(image, 0, 0, null);
		graphics.dispose();
		return bufferedImage;
	}

	private static String mimeType(final File file, final String extension) throws IOException {
		if ("svg".equals(extension)) {
			return "image/svg+xml";
		}
		final String detectedType = Files.probeContentType(file.toPath());
		if (detectedType != null && detectedType.startsWith("image/")) {
			return detectedType;
		}
		if ("jpg".equals(extension) || "jpeg".equals(extension)) {
			return "image/jpeg";
		}
		if ("gif".equals(extension)) {
			return "image/gif";
		}
		if ("webp".equals(extension)) {
			return "image/webp";
		}
		if ("bmp".equals(extension)) {
			return "image/bmp";
		}
		return "image/" + extension;
	}

	private static Dimension imageSize(final File file, final String extension) throws IOException {
		if ("svg".equals(extension)) {
			return null;
		}
		final Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix(extension);
		if (!readers.hasNext()) {
			return null;
		}
		final ImageReader reader = readers.next();
		try (ImageInputStream inputStream = ImageIO.createImageInputStream(file)) {
			if (inputStream == null) {
				return null;
			}
			reader.setInput(inputStream);
			return new Dimension(reader.getWidth(0), reader.getHeight(0));
		}
		finally {
			reader.dispose();
		}
	}

	private static String sha256(final byte[] bytes) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hash = digest.digest(bytes);
			final StringBuilder builder = new StringBuilder(hash.length * 2);
			for (final byte value : hash) {
				final String hex = Integer.toHexString(value & 0xff);
				if (hex.length() == 1) {
					builder.append('0');
				}
				builder.append(hex);
			}
			return builder.toString();
		}
		catch (final NoSuchAlgorithmException e) {
			throw new IllegalStateException(e);
		}
	}
}
