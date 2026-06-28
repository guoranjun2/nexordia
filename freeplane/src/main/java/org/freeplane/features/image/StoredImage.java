package org.freeplane.features.image;

public class StoredImage {
	private final String source;
	private final String fileName;
	private final int width;
	private final int height;

	StoredImage(final String source, final String fileName, final int width, final int height) {
		this.source = source;
		this.fileName = fileName;
		this.width = width;
		this.height = height;
	}

	public String getSource() {
		return source;
	}

	public String getFileName() {
		return fileName;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean hasKnownSize() {
		return width > 0 && height > 0;
	}
}
