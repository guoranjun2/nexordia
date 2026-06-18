/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2026
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
package org.freeplane.view.swing.map.cloud;

import org.freeplane.core.resources.ResourceController;

public class CloudPaintingOptions {
	public static final String PAINT_CLOUD_PROPERTY = "paint_cloud";
	public static final String CLOUD_PAINTING_MIN_WIDTH_PROPERTY = "cloud_painting_min_width";
	public static final String CLOUD_TEXT_PAINTING_MIN_WIDTH_PROPERTY = "cloud_text_painting_min_width";
	public static final String PAINT_CLOUD_FOR_INVISIBLE_NODE_PROPERTY = "paint_cloud_for_invisible_node";
	public static final String PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE_PROPERTY = "paint_cloud_title_for_invisible_node";
	public static final String PAINT_CLOUD_IMAGE_PROPERTY = "paint_cloud_image";
	public static final String PAINT_CLOUD_TEXT_PROPERTY = "paint_cloud_text";
	public static final String CLOUD_IMAGE_FIXED_ICON_PROPERTY = "cloud_image_fixed_icon";
	public static final String CLOUD_IMAGE_OPACITY_PROPERTY = "cloud_image_opacity";

	private static final int DEFAULT_CLOUD_PAINTING_MIN_WIDTH = 10;
	private static final int DEFAULT_CLOUD_TEXT_PAINTING_MIN_WIDTH = 10;
	private static final boolean DEFAULT_PAINT_CLOUD = true;
	private static final boolean DEFAULT_PAINT_CLOUD_FOR_INVISIBLE_NODE = true;
	private static final boolean DEFAULT_PAINT_CLOUD_IMAGE = true;
	private static final boolean DEFAULT_PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE = false;
	private static final boolean DEFAULT_PAINT_CLOUD_TEXT = true;
	private static final String DEFAULT_CLOUD_IMAGE_FIXED_ICON = "info";
	private static final double DEFAULT_CLOUD_IMAGE_OPACITY = 1d;
	private static final double MINIMUM_CLOUD_IMAGE_OPACITY = 0.05d;
	private static final double MAXIMUM_CLOUD_IMAGE_OPACITY = 1d;

	public static boolean isCloudPaintingEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_PROPERTY, DEFAULT_PAINT_CLOUD);
	}

	public static int getCloudPaintingMinWidth() {
		return ResourceController.getResourceController().getIntProperty(CLOUD_PAINTING_MIN_WIDTH_PROPERTY,
				DEFAULT_CLOUD_PAINTING_MIN_WIDTH);
	}

	public static int getCloudTextPaintingMinWidth() {
		return ResourceController.getResourceController().getIntProperty(CLOUD_TEXT_PAINTING_MIN_WIDTH_PROPERTY,
				DEFAULT_CLOUD_TEXT_PAINTING_MIN_WIDTH);
	}

	public static boolean isCloudPaintingForInvisibleNodeEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_FOR_INVISIBLE_NODE_PROPERTY,
				DEFAULT_PAINT_CLOUD_FOR_INVISIBLE_NODE);
	}

	public static boolean isCloudTitlePaintingForInvisibleNodeEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE_PROPERTY,
				DEFAULT_PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE);
	}

	public static boolean isCloudTextPaintingEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_TEXT_PROPERTY,
				DEFAULT_PAINT_CLOUD_TEXT);
	}

	public static boolean isCloudImagePaintingEnabled() {
		return ResourceController.getResourceController().getBooleanProperty(PAINT_CLOUD_IMAGE_PROPERTY,
				DEFAULT_PAINT_CLOUD_IMAGE);
	}

	public static String getCloudImageFixedIcon() {
		return ResourceController.getResourceController().getProperty(CLOUD_IMAGE_FIXED_ICON_PROPERTY,
				DEFAULT_CLOUD_IMAGE_FIXED_ICON);
	}

	public static double getCloudImageOpacity() {
		final double opacity = ResourceController.getResourceController().getDoubleProperty(CLOUD_IMAGE_OPACITY_PROPERTY,
				DEFAULT_CLOUD_IMAGE_OPACITY);
		return CloudImagePainting.opacityInRange(opacity, MINIMUM_CLOUD_IMAGE_OPACITY, MAXIMUM_CLOUD_IMAGE_OPACITY);
	}

	public static boolean isCloudPaintingProperty(String propertyName) {
		return PAINT_CLOUD_PROPERTY.equals(propertyName)
				|| CLOUD_PAINTING_MIN_WIDTH_PROPERTY.equals(propertyName)
				|| CLOUD_TEXT_PAINTING_MIN_WIDTH_PROPERTY.equals(propertyName)
				|| PAINT_CLOUD_FOR_INVISIBLE_NODE_PROPERTY.equals(propertyName)
				|| PAINT_CLOUD_TITLE_FOR_INVISIBLE_NODE_PROPERTY.equals(propertyName)
				|| PAINT_CLOUD_IMAGE_PROPERTY.equals(propertyName)
				|| PAINT_CLOUD_TEXT_PROPERTY.equals(propertyName)
				|| CLOUD_IMAGE_FIXED_ICON_PROPERTY.equals(propertyName)
				|| CLOUD_IMAGE_OPACITY_PROPERTY.equals(propertyName);
	}

	private CloudPaintingOptions() {
	}
}
