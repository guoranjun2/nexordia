package org.freeplane.view.swing.map.cloud;

class CloudImagePainting {
	static boolean shouldPaint(boolean enabled, boolean visible, boolean simplified, boolean fixedImageCloud) {
		return enabled && ((visible && simplified) || fixedImageCloud);
	}

	static boolean shouldKeepTextWithFixedImage(boolean imagePaintingEnabled, boolean hasFixedIcon, boolean hasImage) {
		return imagePaintingEnabled && hasFixedIcon && hasImage;
	}

	static double opacityInRange(double opacity, double minimum, double maximum) {
		return Math.max(minimum, Math.min(maximum, opacity));
	}

	private CloudImagePainting() {
	}
}
