package org.freeplane.view.swing.map;

class ZoomCalculator {
	static float calculate(float oldZoom, double direction, float zoomShift) {
		if (direction == 0)
			return oldZoom;
		final float shift = Math.max(0.01f, Math.min(zoomShift, 0.5f));
		final float zoomFactor = direction > 0 ? 1f - shift : 1f + shift;
		final float zoom = oldZoom * zoomFactor;
		return Math.min(zoom, 32f);
	}
}
