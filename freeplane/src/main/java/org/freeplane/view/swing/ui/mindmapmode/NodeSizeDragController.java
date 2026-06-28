package org.freeplane.view.swing.ui.mindmapmode;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;

import org.freeplane.api.LengthUnit;
import org.freeplane.api.Quantity;
import org.freeplane.features.image.ImageHtmlInserter;
import org.freeplane.features.image.ImageHtmlInserter.ImageSize;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.nodestyle.NodeStyleController;
import org.freeplane.features.nodestyle.mindmapmode.MNodeStyleController;
import org.freeplane.features.text.mindmapmode.MTextController;
import org.freeplane.view.swing.ui.HtmlImageHitDetector;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

class NodeSizeDragController {
	private static final int EDGE_INNER_SENSITIVITY = 5;
	private static final int EDGE_OUTER_SENSITIVITY = 12;
	private static final int CLICK_TOLERANCE = 3;
	private static final int MINIMUM_NODE_RESIZE_VIEW_SIZE = 14;
	private static final int MINIMUM_IMAGE_RESIZE_VIEW_SIZE = 24;
	private static final int MINIMUM_NODE_WIDTH = 20;
	private static final int MINIMUM_IMAGE_SIZE = 16;
	private static final int MINIMUM_FONT_SIZE = 1;
	private static final float MINIMUM_RESIZE_ZOOM = 0.35f;

	private final HtmlImageHitDetector hitDetector = new HtmlImageHitDetector();
	private final ImageHtmlInserter htmlInserter = new ImageHtmlInserter();
	private ActiveDrag activeDrag;
	private boolean resizeCursorShown;

	static boolean isResizeHandle(final MainView mainView, final Point point) {
		return new NodeSizeDragController().dragCandidate(mainView, point) != null;
	}

	boolean isActive() {
		return activeDrag != null;
	}

	boolean mouseMoved(final MouseEvent e) {
		if (activeDrag != null || e.getClickCount() != 0) {
			return activeDrag != null;
		}
		final MainView mainView = (MainView) e.getSource();
		final DragCandidate candidate = dragCandidate(mainView, e.getPoint());
		if (candidate == null) {
			resizeCursorShown = false;
			return false;
		}
		mainView.setCursor(Cursor.getPredefinedCursor(candidate.cursorType()));
		resizeCursorShown = true;
		return true;
	}

	boolean mousePressed(final MouseEvent e) {
		if (!isPlainLeftButtonPress(e)) {
			return false;
		}
		final MainView mainView = (MainView) e.getSource();
		final DragCandidate candidate = dragCandidate(mainView, e.getPoint());
		if (candidate == null) {
			return false;
		}
		activeDrag = ActiveDrag.start(mainView, candidate, e, htmlInserter);
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
		else {
			resetClickedHandle();
		}
		activeDrag = null;
		e.consume();
		return true;
	}

	void mouseExited(final MouseEvent e) {
		if (activeDrag == null && resizeCursorShown) {
			((MainView) e.getSource()).setCursor(Cursor.getDefaultCursor());
			resizeCursorShown = false;
		}
	}

	private boolean isPlainLeftButtonPress(final MouseEvent e) {
		return e.getButton() == MouseEvent.BUTTON1
				&& (e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) == InputEvent.BUTTON1_DOWN_MASK
				&& !e.isAltDown()
				&& !e.isControlDown()
				&& !e.isMetaDown()
				&& !e.isShiftDown();
	}

	private DragCandidate dragCandidate(final MainView mainView, final Point point) {
		final NodeView nodeView = mainView.getNodeView();
		final MapView map = nodeView.getMap();
		final ModeController modeController = map.getModeController();
		if (!hasUsableResizeTarget(mainView, map) || !modeController.canEdit(map.getMap())
				|| mainView.isInFoldingRegion(point)) {
			return null;
		}
		final DragCandidate imageCandidate = imageDragCandidate(mainView, point);
		return imageCandidate != null ? imageCandidate : nodeDragCandidate(mainView, point);
	}

	private boolean hasUsableResizeTarget(final MainView mainView, final MapView map) {
		return map.getZoom() >= MINIMUM_RESIZE_ZOOM
				|| mainView.getWidth() >= MINIMUM_NODE_RESIZE_VIEW_SIZE
				&& mainView.getHeight() >= MINIMUM_NODE_RESIZE_VIEW_SIZE;
	}

	private DragCandidate imageDragCandidate(final MainView mainView, final Point point) {
		final HtmlImageHitDetector.Hit hit = hitDetector.findImageNear(mainView, point, EDGE_OUTER_SENSITIVITY);
		if (hit == null || hit.getBounds().width < MINIMUM_IMAGE_RESIZE_VIEW_SIZE
				|| hit.getBounds().height < MINIMUM_IMAGE_RESIZE_VIEW_SIZE) {
			return null;
		}
		final DragEdge edge = edgeAt(hit.getBounds(), point);
		return edge != null ? new DragCandidate(edge.imageMode(), hit.getBounds(), hit.getImageIndex()) : null;
	}

	private DragCandidate nodeDragCandidate(final MainView mainView, final Point point) {
		final Rectangle bounds = new Rectangle(0, 0, mainView.getWidth(), mainView.getHeight());
		if (isBottomRightCorner(bounds, point)) {
			return new DragCandidate(DragMode.NODE_WIDTH_AND_FONT_BOTTOM_RIGHT, bounds);
		}
		final DragEdge edge = edgeAt(bounds, point);
		return edge != null ? new DragCandidate(edge.nodeMode(), bounds) : null;
	}

	private boolean isBottomRightCorner(final Rectangle bounds, final Point point) {
		final int right = bounds.x + bounds.width;
		final int bottom = bounds.y + bounds.height;
		return point.x >= right - EDGE_INNER_SENSITIVITY
				&& point.x <= right + EDGE_OUTER_SENSITIVITY
				&& point.y >= bottom - EDGE_INNER_SENSITIVITY
				&& point.y <= bottom + EDGE_OUTER_SENSITIVITY;
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
		switch (activeDrag.mode) {
			case NODE_WIDTH_LEFT:
			case NODE_WIDTH_RIGHT:
				updateNodeWidthDrag(e);
				break;
			case NODE_FONT_TOP:
			case NODE_FONT_BOTTOM:
				updateFontDrag(e);
				break;
			case NODE_WIDTH_AND_FONT_BOTTOM_RIGHT:
				updateNodeWidthDrag(e);
				updateFontDrag(e);
				break;
			case IMAGE_LEFT:
			case IMAGE_RIGHT:
			case IMAGE_TOP:
			case IMAGE_BOTTOM:
				updateImageDrag(e);
				break;
		}
	}

	private void updateNodeWidthDrag(final MouseEvent e) {
		final int deltaX = e.getXOnScreen() - activeDrag.startScreenX;
		final int direction = activeDrag.mode == DragMode.NODE_WIDTH_LEFT ? -1 : 1;
		final int width = Math.max(MINIMUM_NODE_WIDTH,
				activeDrag.startNodeWidth + Math.round(direction * deltaX / activeDrag.zoom));
		activeDrag.currentNodeWidth = width;
		setLiveNodeWidth(activeDrag.mainView, width);
	}

	private void updateFontDrag(final MouseEvent e) {
		final int deltaY = e.getYOnScreen() - activeDrag.startScreenY;
		final int direction = activeDrag.mode == DragMode.NODE_FONT_TOP ? -1 : 1;
		final double factor = (activeDrag.startViewHeight + direction * deltaY) / (double) activeDrag.startViewHeight;
		final int fontSize = boundedFontSize((int) Math.round(activeDrag.startFontSize * factor));
		setLiveFontSize(fontSize);
	}

	private void setLiveFontSize(final int fontSize) {
		if (fontSize == activeDrag.currentFontSize) {
			return;
		}
		activeDrag.currentFontSize = fontSize;
		final float displayFactor = fontSize / (float) activeDrag.startFontSize;
		final Font font = activeDrag.startDisplayFont.deriveFont(activeDrag.startDisplayFont.getSize2D() * displayFactor);
		activeDrag.mainView.setFont(font);
		activeDrag.mainView.revalidate();
		activeDrag.mainView.repaint();
	}

	private void updateImageDrag(final MouseEvent e) {
		final int deltaX = e.getXOnScreen() - activeDrag.startScreenX;
		final int deltaY = e.getYOnScreen() - activeDrag.startScreenY;
		final double factor;
		switch (activeDrag.mode) {
			case IMAGE_LEFT:
				factor = (activeDrag.startImageWidth - deltaX / activeDrag.zoom) / (double) activeDrag.startImageWidth;
				break;
			case IMAGE_RIGHT:
				factor = (activeDrag.startImageWidth + deltaX / activeDrag.zoom) / (double) activeDrag.startImageWidth;
				break;
			case IMAGE_TOP:
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
		final String html = htmlInserter.resizeImage(activeDrag.originalText, activeDrag.imageIndex, width, height);
		updateNodeTextPreview(html);
		if (activeDrag.imageOnly) {
			activeDrag.currentNodeWidth = width;
			setLiveNodeWidth(activeDrag.mainView, width);
		}
	}

	private double minimumImageFactor() {
		return Math.max(MINIMUM_IMAGE_SIZE / (double) activeDrag.startImageWidth,
				MINIMUM_IMAGE_SIZE / (double) activeDrag.startImageHeight);
	}

	private void updateNodeTextPreview(final String html) {
		final Object previous = activeDrag.node.getUserObject();
		if (html.equals(previous)) {
			return;
		}
		activeDrag.node.setUserObject(html);
		activeDrag.modeController.getMapController().nodeChanged(activeDrag.node, NodeModel.NODE_TEXT, previous, html);
		activeDrag.previewText = html;
	}

	private void setLiveNodeWidth(final MainView mainView, final int width) {
		final MapView map = mainView.getNodeView().getMap();
		final Quantity<LengthUnit> widthQuantity = LengthUnit.pixelsInPt(width);
		final int zoomedWidth = map.getZoomed(widthQuantity.toBaseUnits());
		mainView.setMinimumWidth(zoomedWidth);
		mainView.setMaximumWidth(zoomedWidth);
		mainView.revalidate();
		mainView.repaint();
	}

	private void finishDrag() {
		switch (activeDrag.mode) {
			case NODE_WIDTH_LEFT:
			case NODE_WIDTH_RIGHT:
				commitNodeWidth(activeDrag.currentNodeWidth);
				break;
			case NODE_FONT_TOP:
			case NODE_FONT_BOTTOM:
				commitFontSize(activeDrag.currentFontSize);
				break;
			case NODE_WIDTH_AND_FONT_BOTTOM_RIGHT:
				commitNodeWidth(activeDrag.currentNodeWidth);
				commitFontSize(activeDrag.currentFontSize);
				break;
			case IMAGE_LEFT:
			case IMAGE_RIGHT:
			case IMAGE_TOP:
			case IMAGE_BOTTOM:
				commitImageSize();
				break;
		}
		activeDrag.mainView.setCursor(Cursor.getDefaultCursor());
		resizeCursorShown = false;
	}

	private void resetClickedHandle() {
		switch (activeDrag.mode) {
			case NODE_WIDTH_LEFT:
			case NODE_WIDTH_RIGHT:
				resetNodeWidth();
				break;
			case NODE_FONT_TOP:
			case NODE_FONT_BOTTOM:
				resetFontSize();
				break;
			case NODE_WIDTH_AND_FONT_BOTTOM_RIGHT:
				resetNodeWidth();
				resetFontSize();
				break;
			default:
				break;
		}
		activeDrag.mainView.setCursor(Cursor.getDefaultCursor());
		resizeCursorShown = false;
	}

	private void commitNodeWidth(final int width) {
		final Quantity<LengthUnit> widthQuantity = LengthUnit.pixelsInPt(width);
		final MNodeStyleController styleController = styleController();
		if (styleController == null) {
			return;
		}
		styleController.setMinNodeWidth(activeDrag.node, widthQuantity);
		styleController.setMaxNodeWidth(activeDrag.node, widthQuantity);
	}

	private void commitFontSize(final int fontSize) {
		final MNodeStyleController styleController = styleController();
		if (styleController != null) {
			styleController.setFontSize(activeDrag.node, fontSize);
		}
	}

	private void resetNodeWidth() {
		final MNodeStyleController styleController = styleController();
		if (styleController == null) {
			return;
		}
		styleController.setMinNodeWidth(activeDrag.node, null);
		styleController.setMaxNodeWidth(activeDrag.node, null);
	}

	private void resetFontSize() {
		final MNodeStyleController styleController = styleController();
		if (styleController != null) {
			styleController.setFontSize(activeDrag.node, null);
		}
	}

	private void commitImageSize() {
		final String finalText = activeDrag.previewText;
		if (!finalText.equals(activeDrag.originalText)) {
			activeDrag.node.setUserObject(activeDrag.originalText);
			activeDrag.modeController.getMapController().nodeChanged(activeDrag.node, NodeModel.NODE_TEXT, finalText,
					activeDrag.originalText);
			MTextController.getController().setNodeText(activeDrag.node, finalText);
		}
		if (activeDrag.imageOnly) {
			commitNodeWidth(activeDrag.currentNodeWidth);
		}
	}

	private MNodeStyleController styleController() {
		return (MNodeStyleController) activeDrag.modeController.getExtension(NodeStyleController.class);
	}

	private int boundedFontSize(final int fontSize) {
		return Math.max(MINIMUM_FONT_SIZE, fontSize);
	}

	private enum DragEdge {
		LEFT {
			@Override
			DragMode nodeMode() {
				return DragMode.NODE_WIDTH_LEFT;
			}

			@Override
			DragMode imageMode() {
				return DragMode.IMAGE_LEFT;
			}
		},
		RIGHT {
			@Override
			DragMode nodeMode() {
				return DragMode.NODE_WIDTH_RIGHT;
			}

			@Override
			DragMode imageMode() {
				return DragMode.IMAGE_RIGHT;
			}
		},
		TOP {
			@Override
			DragMode nodeMode() {
				return DragMode.NODE_FONT_TOP;
			}

			@Override
			DragMode imageMode() {
				return DragMode.IMAGE_TOP;
			}
		},
		BOTTOM {
			@Override
			DragMode nodeMode() {
				return DragMode.NODE_FONT_BOTTOM;
			}

			@Override
			DragMode imageMode() {
				return DragMode.IMAGE_BOTTOM;
			}
		};

		abstract DragMode nodeMode();

		abstract DragMode imageMode();
	}

	private enum DragMode {
		NODE_WIDTH_LEFT(Cursor.W_RESIZE_CURSOR),
		NODE_WIDTH_RIGHT(Cursor.E_RESIZE_CURSOR),
		NODE_FONT_TOP(Cursor.N_RESIZE_CURSOR),
		NODE_FONT_BOTTOM(Cursor.S_RESIZE_CURSOR),
		NODE_WIDTH_AND_FONT_BOTTOM_RIGHT(Cursor.SE_RESIZE_CURSOR),
		IMAGE_LEFT(Cursor.W_RESIZE_CURSOR),
		IMAGE_RIGHT(Cursor.E_RESIZE_CURSOR),
		IMAGE_TOP(Cursor.N_RESIZE_CURSOR),
		IMAGE_BOTTOM(Cursor.S_RESIZE_CURSOR);

		private final int cursorType;

		DragMode(final int cursorType) {
			this.cursorType = cursorType;
		}

		boolean isImage() {
			switch (this) {
				case IMAGE_LEFT:
				case IMAGE_RIGHT:
				case IMAGE_TOP:
				case IMAGE_BOTTOM:
					return true;
				default:
					return false;
			}
		}
	}

	private static class DragCandidate {
		private final DragMode mode;
		private final Rectangle bounds;
		private final int imageIndex;

		DragCandidate(final DragMode mode, final Rectangle bounds) {
			this(mode, bounds, -1);
		}

		DragCandidate(final DragMode mode, final Rectangle bounds, final int imageIndex) {
			this.mode = mode;
			this.bounds = bounds;
			this.imageIndex = imageIndex;
		}

		int cursorType() {
			return mode.cursorType;
		}
	}

	private static class ActiveDrag {
		private final MainView mainView;
		private final NodeModel node;
		private final ModeController modeController;
		private final DragMode mode;
		private final int startScreenX;
		private final int startScreenY;
		private final float zoom;
		private final int startViewHeight;
		private final int startNodeWidth;
		private final int startFontSize;
		private final Font startDisplayFont;
		private final String originalText;
		private final boolean imageOnly;
		private final int imageIndex;
		private final int startImageWidth;
		private final int startImageHeight;
		private int currentNodeWidth;
		private int currentFontSize;
		private int currentImageWidth;
		private int currentImageHeight;
		private String previewText;
		private boolean dragged;

		static ActiveDrag start(final MainView mainView, final DragCandidate candidate, final MouseEvent e,
				final ImageHtmlInserter htmlInserter) {
			final NodeView nodeView = mainView.getNodeView();
			final MapView map = nodeView.getMap();
			final NodeModel node = nodeView.getNode();
			final int fontSize = NodeStyleController.getController(map.getModeController())
					.getFontSize(node, nodeView.getStyleOption());
			final ImageSize imageSize = imageSize(candidate, node.getText(), map.getZoom(), htmlInserter);
			if (candidate.mode.isImage() && imageSize == null) {
				return null;
			}
			return new ActiveDrag(mainView, node, map.getModeController(), candidate.mode, e.getXOnScreen(),
					e.getYOnScreen(), map.getZoom(), mainView.getHeight(),
					Math.max(MINIMUM_NODE_WIDTH, Math.round(mainView.getWidth() / map.getZoom())),
					fontSize, mainView.getFont(), node.getText(), htmlInserter.isImageOnly(node.getText()),
					candidate.imageIndex, imageSize != null ? imageSize.getWidth() : -1,
					imageSize != null ? imageSize.getHeight() : -1);
		}

		private static ImageSize imageSize(final DragCandidate candidate, final String text, final float zoom,
				final ImageHtmlInserter htmlInserter) {
			if (!candidate.mode.isImage()) {
				return null;
			}
			final ImageSize imageSize = htmlInserter.imageSize(text, candidate.imageIndex);
			if (imageSize != null) {
				return imageSize;
			}
			return new ImageSize(Math.max(MINIMUM_IMAGE_SIZE, Math.round(candidate.bounds.width / zoom)),
					Math.max(MINIMUM_IMAGE_SIZE, Math.round(candidate.bounds.height / zoom)));
		}

		private ActiveDrag(final MainView mainView, final NodeModel node, final ModeController modeController,
				final DragMode mode, final int startScreenX, final int startScreenY, final float zoom,
				final int startViewHeight, final int startNodeWidth, final int startFontSize,
				final Font startDisplayFont, final String originalText, final boolean imageOnly,
				final int imageIndex, final int startImageWidth, final int startImageHeight) {
			this.mainView = mainView;
			this.node = node;
			this.modeController = modeController;
			this.mode = mode;
			this.startScreenX = startScreenX;
			this.startScreenY = startScreenY;
			this.zoom = zoom;
			this.startViewHeight = Math.max(1, startViewHeight);
			this.startNodeWidth = startNodeWidth;
			this.startFontSize = Math.max(1, startFontSize);
			this.startDisplayFont = startDisplayFont;
			this.originalText = originalText;
			this.imageOnly = imageOnly;
			this.imageIndex = imageIndex;
			this.startImageWidth = startImageWidth;
			this.startImageHeight = startImageHeight;
			this.currentNodeWidth = startNodeWidth;
			this.currentFontSize = this.startFontSize;
			this.currentImageWidth = startImageWidth;
			this.currentImageHeight = startImageHeight;
			this.previewText = originalText;
			this.dragged = false;
		}

		private boolean hasMovedBeyondClick(final MouseEvent e) {
			return Math.abs(e.getXOnScreen() - startScreenX) > CLICK_TOLERANCE
					|| Math.abs(e.getYOnScreen() - startScreenY) > CLICK_TOLERANCE;
		}
	}
}
