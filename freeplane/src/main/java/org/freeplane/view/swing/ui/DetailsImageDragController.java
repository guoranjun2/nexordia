package org.freeplane.view.swing.ui;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import org.freeplane.core.util.HtmlUtils;
import org.freeplane.features.image.ImageHtmlInserter;
import org.freeplane.features.image.ImageHtmlInserter.ImageSize;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.text.DetailModel;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;
import org.freeplane.view.swing.map.ZoomableLabel;

class DetailsImageDragController {
	private static final int EDGE_INNER_SENSITIVITY = 5;
	private static final int EDGE_OUTER_SENSITIVITY = 12;
	private static final int CLICK_TOLERANCE = 3;
	private static final int MINIMUM_VIEW_SIZE = 24;
	private static final int MINIMUM_IMAGE_SIZE = 16;
	private static final float MINIMUM_RESIZE_ZOOM = 0.35f;

	private final HtmlImageHitDetector hitDetector = new HtmlImageHitDetector();
	private final ImageHtmlInserter htmlInserter = new ImageHtmlInserter();
	private ActiveDrag activeDrag;
	private boolean resizeCursorShown;

	boolean isActive() {
		return activeDrag != null;
	}

	boolean mouseMoved(final MouseEvent e) {
		if (activeDrag != null || e.getClickCount() != 0) {
			return activeDrag != null;
		}
		final ZoomableLabel label = (ZoomableLabel) e.getSource();
		final DragCandidate candidate = dragCandidate(label, e.getPoint());
		if (candidate == null) {
			resizeCursorShown = false;
			return false;
		}
		label.setCursor(Cursor.getPredefinedCursor(candidate.cursorType()));
		resizeCursorShown = true;
		return true;
	}

	boolean mousePressed(final MouseEvent e) {
		if (!isPlainLeftButtonPress(e)) {
			return false;
		}
		final ZoomableLabel label = (ZoomableLabel) e.getSource();
		final DragCandidate candidate = dragCandidate(label, e.getPoint());
		if (candidate == null) {
			return false;
		}
		activeDrag = ActiveDrag.start(label, candidate, e, htmlInserter);
		if (activeDrag == null) {
			return false;
		}
		e.consume();
		return true;
	}

	boolean mouseDragged(final MouseEvent e) {
		if (activeDrag == null) {
			return false;
		}
		if ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK) {
			if (activeDrag.hasMovedBeyondClick(e)) {
				activeDrag.dragged = true;
				updateDrag(e);
			}
		}
		e.consume();
		return true;
	}

	boolean mouseReleased(final MouseEvent e) {
		if (activeDrag == null) {
			return false;
		}
		if (activeDrag.dragged) {
			finishDrag();
		}
		resetCursor();
		activeDrag = null;
		e.consume();
		return true;
	}

	void mouseExited(final MouseEvent e) {
		if (activeDrag == null && resizeCursorShown) {
			((ZoomableLabel) e.getSource()).setCursor(Cursor.getDefaultCursor());
			resizeCursorShown = false;
		}
	}

	private DragCandidate dragCandidate(final ZoomableLabel label, final Point point) {
		final NodeView nodeView = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, label);
		if (nodeView == null || !hasUsableResizeTarget(label, nodeView)) {
			return null;
		}
		final MapView map = nodeView.getMap();
		final ModeController modeController = map.getModeController();
		if (!modeController.canEdit(map.getMap())) {
			return null;
		}
		final String details = DetailModel.getDetailText(nodeView.getNode());
		final HtmlImageHitDetector.Hit hit = hitDetector.findImageNear(label, point, EDGE_OUTER_SENSITIVITY, details);
		if (hit == null || hit.getBounds().width < MINIMUM_VIEW_SIZE || hit.getBounds().height < MINIMUM_VIEW_SIZE) {
			return null;
		}
		final DragEdge edge = edgeAt(hit.getBounds(), point);
		return edge != null ? new DragCandidate(edge, hit) : null;
	}

	private boolean hasUsableResizeTarget(final ZoomableLabel label, final NodeView nodeView) {
		return nodeView.getMap().getZoom() >= MINIMUM_RESIZE_ZOOM
				&& label.getWidth() >= MINIMUM_VIEW_SIZE
				&& label.getHeight() >= MINIMUM_VIEW_SIZE;
	}

	private boolean isPlainLeftButtonPress(final MouseEvent e) {
		return e.getButton() == MouseEvent.BUTTON1
				&& (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK
				&& !e.isAltDown()
				&& !e.isControlDown()
				&& !e.isMetaDown()
				&& !e.isShiftDown();
	}

	private DragEdge edgeAt(final Rectangle bounds, final Point point) {
		DragEdge edge = null;
		int nearest = Integer.MAX_VALUE;
		if (isInVerticalBand(bounds, point) && isInLeftBand(bounds, point)) {
			nearest = Math.abs(point.x - bounds.x);
			edge = DragEdge.LEFT;
		}
		if (isInVerticalBand(bounds, point) && isInRightBand(bounds, point)) {
			final int distance = Math.abs(point.x - (bounds.x + bounds.width));
			if (distance < nearest) {
				nearest = distance;
				edge = DragEdge.RIGHT;
			}
		}
		if (isInHorizontalBand(bounds, point) && isInTopBand(bounds, point)) {
			final int distance = Math.abs(point.y - bounds.y);
			if (distance < nearest) {
				nearest = distance;
				edge = DragEdge.TOP;
			}
		}
		if (isInHorizontalBand(bounds, point) && isInBottomBand(bounds, point)) {
			final int distance = Math.abs(point.y - (bounds.y + bounds.height));
			if (distance < nearest) {
				edge = DragEdge.BOTTOM;
			}
		}
		return edge;
	}

	private boolean isInVerticalBand(final Rectangle bounds, final Point point) {
		return point.y >= bounds.y - EDGE_OUTER_SENSITIVITY
				&& point.y <= bounds.y + bounds.height + EDGE_OUTER_SENSITIVITY;
	}

	private boolean isInHorizontalBand(final Rectangle bounds, final Point point) {
		return point.x >= bounds.x - EDGE_OUTER_SENSITIVITY
				&& point.x <= bounds.x + bounds.width + EDGE_OUTER_SENSITIVITY;
	}

	private boolean isInLeftBand(final Rectangle bounds, final Point point) {
		return point.x >= bounds.x - EDGE_OUTER_SENSITIVITY
				&& point.x <= bounds.x + EDGE_INNER_SENSITIVITY;
	}

	private boolean isInRightBand(final Rectangle bounds, final Point point) {
		final int right = bounds.x + bounds.width;
		return point.x >= right - EDGE_INNER_SENSITIVITY
				&& point.x <= right + EDGE_OUTER_SENSITIVITY;
	}

	private boolean isInTopBand(final Rectangle bounds, final Point point) {
		return point.y >= bounds.y - EDGE_OUTER_SENSITIVITY
				&& point.y <= bounds.y + EDGE_INNER_SENSITIVITY;
	}

	private boolean isInBottomBand(final Rectangle bounds, final Point point) {
		final int bottom = bounds.y + bounds.height;
		return point.y >= bottom - EDGE_INNER_SENSITIVITY
				&& point.y <= bottom + EDGE_OUTER_SENSITIVITY;
	}

	private void updateDrag(final MouseEvent e) {
		final int deltaX = e.getXOnScreen() - activeDrag.startScreenX;
		final int deltaY = e.getYOnScreen() - activeDrag.startScreenY;
		final double factor;
		switch (activeDrag.mode) {
			case LEFT:
				factor = (activeDrag.startImageWidth - deltaX / activeDrag.zoom) / (double) activeDrag.startImageWidth;
				break;
			case RIGHT:
				factor = (activeDrag.startImageWidth + deltaX / activeDrag.zoom) / (double) activeDrag.startImageWidth;
				break;
			case TOP:
				factor = (activeDrag.startImageHeight - deltaY / activeDrag.zoom) / (double) activeDrag.startImageHeight;
				break;
			default:
				factor = (activeDrag.startImageHeight + deltaY / activeDrag.zoom) / (double) activeDrag.startImageHeight;
				break;
		}
		final double boundedFactor = Math.max(minimumImageFactor(), factor);
		final int width = Math.max(MINIMUM_IMAGE_SIZE, (int) Math.round(activeDrag.startImageWidth * boundedFactor));
		final int height = Math.max(MINIMUM_IMAGE_SIZE, (int) Math.round(activeDrag.startImageHeight * boundedFactor));
		if (width == activeDrag.currentImageWidth && height == activeDrag.currentImageHeight) {
			return;
		}
		activeDrag.currentImageWidth = width;
		activeDrag.currentImageHeight = height;
		final String html = htmlInserter.resizeImage(activeDrag.originalDetails, activeDrag.imageIndex, width, height);
		updatePreview(html);
	}

	private double minimumImageFactor() {
		return Math.max(MINIMUM_IMAGE_SIZE / (double) activeDrag.startImageWidth,
				MINIMUM_IMAGE_SIZE / (double) activeDrag.startImageHeight);
	}

	private void updatePreview(final String html) {
		if (html.equals(activeDrag.previewText)) {
			return;
		}
		activeDrag.previewText = html;
		activeDrag.label.setText(displayHtml(activeDrag.nodeView, html));
		activeDrag.label.revalidate();
		activeDrag.label.repaint();
	}

	private String displayHtml(final NodeView nodeView, final String html) {
		if (html != null && HtmlUtils.isHtml(html) && html.indexOf("<img") >= 0 && html.indexOf("<base ") < 0) {
			return "<html><base href=\"" + nodeView.getMap().getMap().getURL() + "\">" + html.substring(6);
		}
		return html;
	}

	private void finishDrag() {
		if (!activeDrag.previewText.equals(activeDrag.originalDetails)) {
			MTextController.getController().setDetails(activeDrag.node, activeDrag.previewText);
		}
	}

	private void resetCursor() {
		activeDrag.label.setCursor(Cursor.getDefaultCursor());
		resizeCursorShown = false;
	}

	private enum DragEdge {
		LEFT(Cursor.W_RESIZE_CURSOR),
		RIGHT(Cursor.E_RESIZE_CURSOR),
		TOP(Cursor.N_RESIZE_CURSOR),
		BOTTOM(Cursor.S_RESIZE_CURSOR);

		private final int cursorType;

		DragEdge(final int cursorType) {
			this.cursorType = cursorType;
		}
	}

	private static class DragCandidate {
		private final DragEdge mode;
		private final HtmlImageHitDetector.Hit hit;

		DragCandidate(final DragEdge mode, final HtmlImageHitDetector.Hit hit) {
			this.mode = mode;
			this.hit = hit;
		}

		int cursorType() {
			return mode.cursorType;
		}
	}

	private static class ActiveDrag {
		private final ZoomableLabel label;
		private final NodeView nodeView;
		private final NodeModel node;
		private final DragEdge mode;
		private final int imageIndex;
		private final int startScreenX;
		private final int startScreenY;
		private final float zoom;
		private final String originalDetails;
		private final int startImageWidth;
		private final int startImageHeight;
		private int currentImageWidth;
		private int currentImageHeight;
		private String previewText;
		private boolean dragged;

		static ActiveDrag start(final ZoomableLabel label, final DragCandidate candidate, final MouseEvent e,
				final ImageHtmlInserter htmlInserter) {
			final NodeView nodeView = (NodeView) SwingUtilities.getAncestorOfClass(NodeView.class, label);
			final NodeModel node = nodeView.getNode();
			final String details = DetailModel.getDetailText(node);
			final ImageSize imageSize = imageSize(candidate, details, nodeView.getMap().getZoom(), htmlInserter);
			if (details == null || imageSize == null) {
				return null;
			}
			return new ActiveDrag(label, nodeView, node, candidate.mode, candidate.hit.getImageIndex(),
					e.getXOnScreen(), e.getYOnScreen(), nodeView.getMap().getZoom(), details,
					imageSize.getWidth(), imageSize.getHeight());
		}

		private static ImageSize imageSize(final DragCandidate candidate, final String details, final float zoom,
				final ImageHtmlInserter htmlInserter) {
			final ImageSize imageSize = htmlInserter.imageSize(details, candidate.hit.getImageIndex());
			if (imageSize != null) {
				return imageSize;
			}
			return new ImageSize(Math.max(MINIMUM_IMAGE_SIZE, Math.round(candidate.hit.getBounds().width / zoom)),
					Math.max(MINIMUM_IMAGE_SIZE, Math.round(candidate.hit.getBounds().height / zoom)));
		}

		private ActiveDrag(final ZoomableLabel label, final NodeView nodeView, final NodeModel node,
				final DragEdge mode, final int imageIndex, final int startScreenX, final int startScreenY,
				final float zoom, final String originalDetails, final int startImageWidth,
				final int startImageHeight) {
			this.label = label;
			this.nodeView = nodeView;
			this.node = node;
			this.mode = mode;
			this.imageIndex = imageIndex;
			this.startScreenX = startScreenX;
			this.startScreenY = startScreenY;
			this.zoom = zoom;
			this.originalDetails = originalDetails;
			this.startImageWidth = startImageWidth;
			this.startImageHeight = startImageHeight;
			this.currentImageWidth = startImageWidth;
			this.currentImageHeight = startImageHeight;
			this.previewText = originalDetails;
			this.dragged = false;
		}

		private boolean hasMovedBeyondClick(final MouseEvent e) {
			return Math.abs(e.getXOnScreen() - startScreenX) > CLICK_TOLERANCE
					|| Math.abs(e.getYOnScreen() - startScreenY) > CLICK_TOLERANCE;
		}
	}
}
