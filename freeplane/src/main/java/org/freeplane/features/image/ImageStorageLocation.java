package org.freeplane.features.image;

import org.freeplane.core.resources.ResourceController;

public enum ImageStorageLocation {
	CURRENT("current"),
	GLOBAL("global"),
	EMBEDDED("embedded");

	public static final String PROPERTY_NAME = "image_storage_location";
	private final String propertyValue;

	ImageStorageLocation(final String propertyValue) {
		this.propertyValue = propertyValue;
	}

	public String propertyValue() {
		return propertyValue;
	}

	public boolean requiresSavedMap() {
		return this == CURRENT;
	}

	public static ImageStorageLocation fromProperty(final String value) {
		for (final ImageStorageLocation location : values()) {
			if (location.propertyValue.equals(value)) {
				return location;
			}
		}
		return CURRENT;
	}

	public static ImageStorageLocation current() {
		return fromProperty(ResourceController.getResourceController().getProperty(PROPERTY_NAME, CURRENT.propertyValue));
	}
}
