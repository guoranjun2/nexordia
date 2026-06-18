package org.freeplane.core.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class PaintPerformanceMonitor {
	public static final String BACKGROUND = "background";
	public static final String BITMAP_IMAGE = "bitmapImage";
	public static final String CLOUD = "cloud";
	public static final String CLOUD_APPROXIMATE_BOUNDS = "cloudApproximateBounds";
	public static final String CLOUD_COORDINATES = "cloudCoordinates";
	public static final String CLOUD_DECORATION = "cloudDecoration";
	public static final String CLOUD_DRAW = "cloudDraw";
	public static final String CLOUD_FILL = "cloudFill";
	public static final String CLOUD_IMAGE = "cloudImage";
	public static final String CLOUD_IMAGE_TEXT = "cloudImageText";
	public static final String CLOUD_PATH = "cloudPath";
	public static final String CLOUD_TEXT = "cloudText";
	public static final String COORDINATE_AXIS = "axis";
	public static final String EDGE = "edge";
	public static final String EDGE_CREATE = "edgeCreate";
	public static final String LAZY_IMAGE = "lazyImage";
	public static final String LAYOUT_VALIDATE = "layoutValidate";
	public static final String LINKS = "links";
	public static final String LINK_CREATE = "linkCreate";
	public static final String LINK_DRAW = "linkDraw";
	public static final String MAIN_VIEW = "mainView";
	public static final String MAIN_VIEW_FULL = "mainViewFull";
	public static final String MAIN_VIEW_SIMPLIFIED = "mainViewSimplified";
	public static final String MAIN_VIEW_TINY = "mainViewTiny";
	public static final String MAP_CHILDREN = "mapChildren";
	public static final String MAP_COMPONENT = "mapComponent";
	public static final String MODE_CLOUDS = "modeClouds";
	public static final String MODE_CLOUD_TEXTS = "modeCloudTexts";
	public static final String MODE_LINKS = "modeLinks";
	public static final String MODE_NODES = "modeNodes";
	public static final String MODE_SELECTED_NODES = "modeSelectedNodes";
	public static final String NODE_UPDATE = "nodeUpdate";
	public static final String NODE_UPDATE_CONTENT = "nodeUpdateContent";
	public static final String NODE_UPDATE_DETAILS = "nodeUpdateDetails";
	public static final String NODE_UPDATE_VIEWER = "nodeUpdateViewer";
	public static final String UPDATE_ALL_NODES = "updateAllNodes";
	public static final String ZOOM_ANCHOR = "zoomAnchor";
	public static final String ZOOM_BACKGROUND = "zoomBackground";
	public static final String ZOOM_UPDATE = "zoomUpdate";

	public static final boolean ENABLED = Boolean.getBoolean("org.freeplane.performance.paint");

	private static final int DEFAULT_REPORT_INTERVAL = 60;
	private static final long DEFAULT_SLOW_FRAME_NANOS = 20_000_000L;
	private static final ThreadLocal<Frame> CURRENT_FRAME = new ThreadLocal<Frame>();
	private static final ThreadLocal<Frame> CURRENT_TASK = new ThreadLocal<Frame>();
	private static long frameCount;

	public static long start() {
		return ENABLED ? System.nanoTime() : 0;
	}

	public static void record(String name, long startNanos) {
		if(! ENABLED)
			return;
		record(name, System.nanoTime() - startNanos, 1);
	}

	public static void record(String name, long elapsedNanos, int count) {
		if(! ENABLED)
			return;
		final Frame frame = CURRENT_FRAME.get();
		if(frame != null) {
			frame.add(name, elapsedNanos, count);
			return;
		}
		final Frame task = CURRENT_TASK.get();
		if(task != null)
			task.add(name, elapsedNanos, count);
	}

	public static void beginFrame() {
		if(! ENABLED)
			return;
		CURRENT_FRAME.set(new Frame("paint performance frame "));
	}

	public static void endFrame() {
		if(! ENABLED)
			return;
		final Frame frame = CURRENT_FRAME.get();
		CURRENT_FRAME.remove();
		if(frame == null)
			return;
		frame.finish();
		frameCount++;
		if(shouldReport(frame))
			LogUtils.info(frame.report(frameCount));
	}

	public static void beginTask(String label) {
		if(! ENABLED)
			return;
		if(CURRENT_TASK.get() == null)
			CURRENT_TASK.set(new Frame(label));
	}

	public static void endTask() {
		if(! ENABLED)
			return;
		final Frame frame = CURRENT_TASK.get();
		CURRENT_TASK.remove();
		if(frame == null)
			return;
		frame.finish();
		if(frame.totalNanos >= slowFrameNanos())
			LogUtils.info(frame.report(0));
	}

	private static boolean shouldReport(Frame frame) {
		return frameCount % reportInterval() == 0 || frame.totalNanos >= slowFrameNanos();
	}

	private static int reportInterval() {
		return Integer.getInteger("org.freeplane.performance.paint.interval", DEFAULT_REPORT_INTERVAL);
	}

	private static long slowFrameNanos() {
		return Long.getLong("org.freeplane.performance.paint.slowFrameMs", DEFAULT_SLOW_FRAME_NANOS / 1_000_000L) * 1_000_000L;
	}

	private static class Frame {
		private final String label;
		private final long startNanos = System.nanoTime();
		private final Map<String, Measurement> measurements = new LinkedHashMap<String, Measurement>();
		private long totalNanos;

		Frame(String label) {
			this.label = label;
		}

		void add(String name, long elapsedNanos, int count) {
			Measurement measurement = measurements.get(name);
			if(measurement == null) {
				measurement = new Measurement();
				measurements.put(name, measurement);
			}
			measurement.nanos += elapsedNanos;
			measurement.count += count;
		}

		void finish() {
			totalNanos = System.nanoTime() - startNanos;
		}

		String report(long frameNumber) {
			final StringBuilder builder = new StringBuilder(label);
			if(frameNumber > 0)
				builder.append(frameNumber);
			builder.append(": total=").append(toMillis(totalNanos)).append("ms");
			for(Map.Entry<String, Measurement> entry : measurements.entrySet()) {
				final Measurement measurement = entry.getValue();
				builder.append(", ").append(entry.getKey()).append("=")
					.append(toMillis(measurement.nanos)).append("ms/")
					.append(measurement.count);
			}
			return builder.toString();
		}

		private String toMillis(long nanos) {
			return String.format("%.2f", nanos / 1_000_000d);
		}
	}

	private static class Measurement {
		private long nanos;
		private int count;
	}
}
