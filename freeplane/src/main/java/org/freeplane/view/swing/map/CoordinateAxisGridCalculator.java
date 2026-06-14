package org.freeplane.view.swing.map;

class CoordinateAxisGridCalculator {
	private static final double BASE_MAP_STEP = 50d;
	private static final double MIN_SCREEN_STEP = 60d;
	private static final double MAX_SCREEN_STEP = 160d;
	private static final float MIN_EFFECTIVE_ZOOM = 0.000001f;

	static double mapStep(float zoom) {
		final float effectiveZoom = effectiveZoom(zoom);
		double step = BASE_MAP_STEP;
		while(step * effectiveZoom < MIN_SCREEN_STEP)
			step *= 2d;
		while(step * effectiveZoom > MAX_SCREEN_STEP)
			step /= 2d;
		return step;
	}

	static double screenStep(double mapStep, float zoom) {
		return mapStep * effectiveZoom(zoom);
	}

	static double mapCoordinate(double screenOffset, float zoom) {
		return screenOffset / effectiveZoom(zoom);
	}

	private static float effectiveZoom(float zoom) {
		return Math.max(zoom, MIN_EFFECTIVE_ZOOM);
	}
}
