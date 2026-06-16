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
package org.freeplane.view.swing.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.Locale;

import org.freeplane.core.resources.ResourceController;

class MapCoordinateAxisPainter {
    static final String SHOW_COORDINATE_AXIS_PROPERTY = "showCoordinateAxis";
    static final String SHOW_COORDINATE_AXIS_Y_INVERTED_PROPERTY = "showCoordinateAxisYInverted";
    static final String COORDINATE_AXIS_COLOR_PROPERTY = "coordinate_axis_color";
    static final String COORDINATE_AXIS_LABEL_COLOR_PROPERTY = "coordinate_axis_label_color";
    private static final Color DEFAULT_COORDINATE_AXIS_COLOR = new Color(60, 100, 180, 50);
    private static final Color DEFAULT_COORDINATE_AXIS_LABEL_COLOR = new Color(90, 90, 90);
    private static final double COORDINATE_AXIS_LABEL_SCALE = 10d;

    void paint(final Graphics2D g, final Rectangle visibleRect, final Point origin, final float zoom) {
        final ResourceController resourceController = ResourceController.getResourceController();
        if(! resourceController.getBooleanProperty(SHOW_COORDINATE_AXIS_PROPERTY))
            return;
        final double mapGridStep = CoordinateAxisGridCalculator.mapStep(zoom);
        final boolean yAxisInverted = resourceController.getBooleanProperty(SHOW_COORDINATE_AXIS_Y_INVERTED_PROPERTY);
        final Color axisColor = coordinateAxisColor(resourceController);
        final Color labelColor = coordinateAxisLabelColor(resourceController);
        final Graphics2D axisGraphics = (Graphics2D) g.create();
        try {
            axisGraphics.setFont(axisGraphics.getFont().deriveFont(Font.PLAIN, Math.max(9f, axisGraphics.getFont().getSize2D() - 1f)));
            final FontMetrics fontMetrics = axisGraphics.getFontMetrics();
            final int xLabelY = clamp(origin.y - 3, visibleRect.y + fontMetrics.getAscent() + 2,
                    visibleRect.y + visibleRect.height - 4);
            final int yLabelX = clamp(origin.x + 3, visibleRect.x + 3, visibleRect.x + visibleRect.width - 80);
            paintVerticalCoordinateLines(axisGraphics, visibleRect, origin, mapGridStep, zoom, xLabelY, axisColor, labelColor);
            paintHorizontalCoordinateLines(axisGraphics, visibleRect, origin, mapGridStep, zoom, yLabelX, fontMetrics,
                    yAxisInverted, axisColor, labelColor);
            axisGraphics.setColor(axisColor);
            axisGraphics.draw(new Line2D.Double(origin.x, visibleRect.y, origin.x, visibleRect.y + visibleRect.height));
            axisGraphics.draw(new Line2D.Double(visibleRect.x, origin.y, visibleRect.x + visibleRect.width, origin.y));
        }
        finally {
            axisGraphics.dispose();
        }
    }

    private Color coordinateAxisColor(final ResourceController resourceController) {
        final Color color = resourceController.getColorProperty(COORDINATE_AXIS_COLOR_PROPERTY);
        return color != null ? color : DEFAULT_COORDINATE_AXIS_COLOR;
    }

    private Color coordinateAxisLabelColor(final ResourceController resourceController) {
        final Color color = resourceController.getColorProperty(COORDINATE_AXIS_LABEL_COLOR_PROPERTY);
        return color != null ? color : DEFAULT_COORDINATE_AXIS_LABEL_COLOR;
    }

    private void paintVerticalCoordinateLines(final Graphics2D g, final Rectangle visibleRect, final Point origin,
                                             final double mapGridStep, final float zoom, final int labelY,
                                             final Color axisColor, final Color labelColor) {
        final double screenGridStep = CoordinateAxisGridCalculator.screenStep(mapGridStep, zoom);
        final int firstIndex = (int) Math.floor((visibleRect.x - origin.x) / screenGridStep);
        final int lastIndex = (int) Math.ceil((visibleRect.x + visibleRect.width - origin.x) / screenGridStep);
        for (int index = firstIndex; index <= lastIndex; index++) {
            final double x = origin.x + index * screenGridStep;
            g.setColor(axisColor);
            g.draw(new Line2D.Double(x, visibleRect.y, x, visibleRect.y + visibleRect.height));
            g.setColor(labelColor);
            g.drawString(formatCoordinate(index * mapGridStep), (int) Math.round(x) + 3, labelY);
        }
    }

    private void paintHorizontalCoordinateLines(final Graphics2D g, final Rectangle visibleRect, final Point origin,
                                               final double mapGridStep, final float zoom, final int labelX,
                                               final FontMetrics fontMetrics, final boolean yAxisInverted,
                                               final Color axisColor, final Color labelColor) {
        final double screenGridStep = CoordinateAxisGridCalculator.screenStep(mapGridStep, zoom);
        final int firstIndex = (int) Math.floor((visibleRect.y - origin.y) / screenGridStep);
        final int lastIndex = (int) Math.ceil((visibleRect.y + visibleRect.height - origin.y) / screenGridStep);
        for (int index = firstIndex; index <= lastIndex; index++) {
            final double y = origin.y + index * screenGridStep;
            g.setColor(axisColor);
            g.draw(new Line2D.Double(visibleRect.x, y, visibleRect.x + visibleRect.width, y));
            if(index != 0) {
                g.setColor(labelColor);
                final double coordinate = (yAxisInverted ? index : -index) * mapGridStep;
                g.drawString(formatCoordinate(coordinate), labelX, (int) Math.round(y) + fontMetrics.getAscent() + 2);
            }
        }
    }

    private String formatCoordinate(final double coordinate) {
        final double scaledCoordinate = coordinate / COORDINATE_AXIS_LABEL_SCALE;
        final double roundedCoordinate = Math.rint(scaledCoordinate);
        if(Math.abs(scaledCoordinate - roundedCoordinate) < 0.001d)
            return String.valueOf((long) roundedCoordinate);
        return String.format(Locale.ROOT, "%.2f", scaledCoordinate);
    }

    private int clamp(final int value, final int min, final int max) {
        return max < min ? min : Math.max(min, Math.min(value, max));
    }
}
