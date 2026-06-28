package org.freeplane.features.image;

public class StoredImage {
	private final String source;
	private final String fileName;

	StoredImage(final String source, final String fileName) {
		this.source = source;
		this.fileName = fileName;
	}

	public String getSource() {
		return source;
	}

	public String getFileName() {
		return fileName;
	}
}
