/*
 *  Freeplane - mind map editor
 *  Copyright (C) 2008 Joerg Mueller, Daniel Polansky, Christian Foltin, Dimitry Polivaev
 *
 *  This file is modified by Dimitry Polivaev in 2008.
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
package org.freeplane.view.swing.map.edge;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;

import org.freeplane.api.ChildNodesAlignment;
import org.freeplane.api.ChildrenSides;
import org.freeplane.api.Dash;
import org.freeplane.api.LayoutOrientation;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.util.PaintPerformanceMonitor;
import org.freeplane.features.map.FreeNode;
import org.freeplane.features.mode.mindmapmode.MModeController;
import org.freeplane.view.swing.map.MainView;
import org.freeplane.view.swing.map.MainView.ConnectorLocation;
import org.freeplane.view.swing.map.MapView;
import org.freeplane.view.swing.map.NodeView;

/**
 * This class represents a single Edge of a MindMap.
 */
public abstract class EdgeView {
    protected static final BasicStroke DEF_STROKE = new BasicStroke();

    static Stroke ECLIPSED_STROKE = null;

    /*
     * FreeNode 动态接口切换阈值。
     *
     * 1.15 表示：
     *   absDx 明显大于 absDy 时，使用 LEFT/RIGHT；
     *   absDy 明显大于 absDx 时，使用 TOP/BOTTOM；
     *   两者接近时，沿用原始接口方向，避免拖动时频繁跳变。
     */
    private static final double FREE_NODE_PORT_SWITCH_RATIO = 1.15;

    protected static Stroke getEclipsedStroke() {
        if (EdgeView.ECLIPSED_STROKE == null) {
            final float dash[] = { 3.0f, 9.0f };
            EdgeView.ECLIPSED_STROKE = new BasicStroke(
                    3.0f,
                    BasicStroke.CAP_BUTT,
                    BasicStroke.JOIN_MITER,
                    12.0f,
                    dash,
                    0.0f
            );
        }

        return EdgeView.ECLIPSED_STROKE;
    }

    private final NodeView source;
    protected Point start, shapeStart, end;
    private final NodeView target;
    private Color color;
    private Integer width;
    private ConnectorLocation startConnectorLocation;
    private ConnectorLocation endConnectorLocation;
    private int[] dash;
    private final boolean highlightsAscendantEdge;


    public EdgeView(
            final NodeView source,
            final NodeView target,
            final Component paintedComponent,
            boolean highlightsAscendantEdge
    ) {
        final long startNanos = PaintPerformanceMonitor.start();
        this.source = source;
        this.target = target;
        this.highlightsAscendantEdge = highlightsAscendantEdge;

        createStart();

        /*
         * 某些 EdgeView 子类 / 布局情况下，createStart() 可能没有设置 shapeStart。
         * 原版代码很多地方默认 shapeStart 可以等于 start。
         */
        if (shapeStart == null) {
            shapeStart = start;
        }

        if (end != null) {
            UITools.convertPointToAncestor(target.getMainView(), end, paintedComponent);
        }

        if (start != null) {
            UITools.convertPointToAncestor(source.getMainView(), start, paintedComponent);
        }

        if (shapeStart != null && start != shapeStart) {
            UITools.convertPointToAncestor(source.getMainView(), shapeStart, paintedComponent);
        }

        adjustConnectorPortsIfNeeded(paintedComponent);

        if (start != null && end != null) {
            align(start, end);
        }
        PaintPerformanceMonitor.record(PaintPerformanceMonitor.EDGE_CREATE, startNanos);
    }

    public void setShapeStart(Point shapeStart) {
        this.shapeStart = this.start = shapeStart;
    }

    public Point getShapeStart() {
        return shapeStart;
    }

    public void setEnd(Point end) {
        this.end = end;
    }

    public Point getEnd() {
        return end;
    }

    protected void createStart() {
        final MainView mainView = source.getMainView();
        final MainView targetMainView = target.getMainView();

        final ChildNodesAlignment childNodesAlignment = source.getChildNodesAlignment();

        boolean usesHorizontalLayout = source.usesHorizontalLayout();

        if (!usesHorizontalLayout && childNodesAlignment.isStacked()) {
            if (source.childrenSides() == ChildrenSides.BOTH_SIDES) {
                if (childNodesAlignment == ChildNodesAlignment.AFTER_PARENT) {
                    start = mainView.getBottomPoint();
                    startConnectorLocation = ConnectorLocation.BOTTOM;
                }
                else {
                    start = mainView.getTopPoint();
                    startConnectorLocation = ConnectorLocation.TOP;
                }
            }
            else {
                if (target.isTopOrLeft()) {
                    start = mainView.getRightPoint();
                    startConnectorLocation = ConnectorLocation.RIGHT;
                }
                else {
                    start = mainView.getLeftPoint();
                    startConnectorLocation = ConnectorLocation.LEFT;
                }
            }

            shapeStart = start;

            if (target.isTopOrLeft()) {
                end = targetMainView.getRightPoint();
                endConnectorLocation = ConnectorLocation.RIGHT;
            }
            else {
                end = targetMainView.getLeftPoint();
                endConnectorLocation = ConnectorLocation.LEFT;
            }
        }
        else {
            final Point relativeLocation = source.getRelativeLocation(target);
            LayoutOrientation layoutOrientation = source.layoutOrientation();

            relativeLocation.x = -relativeLocation.x + mainView.getWidth() / 2;
            relativeLocation.y = -relativeLocation.y + mainView.getHeight() / 2;

            endConnectorLocation = targetMainView.getConnectorLocation(
                    relativeLocation,
                    layoutOrientation,
                    ChildNodesAlignment.NOT_SET
            );
            end = target.getMainView().getConnectorPoint(relativeLocation, endConnectorLocation);

            relativeLocation.x = -relativeLocation.x + mainView.getWidth() / 2 + end.x;
            relativeLocation.y = -relativeLocation.y + mainView.getHeight() / 2 + end.y;

            if (source.isAutoCompactLayoutEnabled() && usesHorizontalLayout && !source.isRoot()) {
                if (target.isTopOrLeft()) {
                    start = mainView.getTopPoint();
                    startConnectorLocation = ConnectorLocation.TOP;
                }
                else {
                    start = mainView.getBottomPoint();
                    startConnectorLocation = ConnectorLocation.BOTTOM;
                }
            }
            else {
                startConnectorLocation = mainView.getConnectorLocation(
                        relativeLocation,
                        LayoutOrientation.NOT_SET,
                        ChildNodesAlignment.NOT_SET
                );
                start = mainView.getConnectorPoint(relativeLocation, startConnectorLocation);
            }

            final boolean needsSpaceForFoldingMark =
                    source.isAutoCompactLayoutEnabled()
                            && !childNodesAlignment.isStacked()
                            && !source.isRoot();

            if (needsSpaceForFoldingMark) {
                switch (startConnectorLocation) {
                    case LEFT:
                        shapeStart = new Point(start.x - source.getZoomedFoldingMarkHalfWidth(2), start.y);
                        break;
                    case RIGHT:
                        shapeStart = new Point(start.x + source.getZoomedFoldingMarkHalfWidth(2), start.y);
                        break;
                    default:
                        shapeStart = start;
                        break;
                }
            }
            else {
                shapeStart = start;
            }
        }
    }

    protected ConnectorLocation getStartConnectorLocation() {
        return startConnectorLocation;
    }

    protected ConnectorLocation getEndConnectorLocation() {
        return endConnectorLocation;
    }

    protected void setStartConnectorLocation(ConnectorLocation startConnectorLocation) {
        this.startConnectorLocation = startConnectorLocation;
    }

    protected void setEndConnectorLocation(ConnectorLocation endConnectorLocation) {
        this.endConnectorLocation = endConnectorLocation;
    }

    /**
     * 是否为 FreeNode 动态重选入口/出口。
     */
    protected boolean usesDynamicConnectorPortsForFreeNode(boolean targetIsFree) {
        return false;
    }

    /**
     * 对自由节点重新选择边的入口/出口。
     *
     * 原始 createStart() 适合普通思维导图布局；
     * 对 FreeNode 来说，经常会把入口/出口算成 LEFT/RIGHT。
     *
     * 这里根据实际绘制坐标中的 dx/dy 重新选择接口：
     *   横向距离明显更大：LEFT/RIGHT
     *   纵向距离明显更大：TOP/BOTTOM
     */
    protected void adjustConnectorPortsIfNeeded(final Component paintedComponent) {
        final FreeNode freeNode = getFreeNodeExtension();

        if (freeNode == null) {
            return;
        }

        final boolean targetIsFree = freeNode.isActive(getTarget().getNode());
        if (!usesDynamicConnectorPortsForFreeNode(targetIsFree)) {
            return;
        }

        final double dx = end.x - shapeStart.x;
        final double dy = end.y - shapeStart.y;

        final ConnectorLocation[] locations = chooseConnectorLocations(dx, dy);
        applyConnectorLocations(locations[0], locations[1], paintedComponent);
    }

    private FreeNode getFreeNodeExtension() {
        try {
            final MModeController modeController = MModeController.getMModeController();

            if (modeController == null) {
                return null;
            }

            return modeController.getExtension(FreeNode.class);
        }
        catch (Exception e) {
            return null;
        }
    }

    /**
     * 根据 dx/dy 选择新的 start/end 接口。
     *
     * 返回数组：
     *   [0] start location
     *   [1] end location
     */
    protected ConnectorLocation[] chooseConnectorLocations(final double dx, final double dy) {
        final double absDx = Math.abs(dx);
        final double absDy = Math.abs(dy);

        final boolean horizontal;

        if (absDx > absDy * FREE_NODE_PORT_SWITCH_RATIO) {
            horizontal = true;
        }
        else if (absDy > absDx * FREE_NODE_PORT_SWITCH_RATIO) {
            horizontal = false;
        }
        else {
            /*
             * 接近 45 度时沿用原始方向，避免接口频繁跳变。
             */
            horizontal = isHorizontalConnectorLocation(startConnectorLocation);
        }

        if (horizontal) {
            if (dx >= 0) {
                return new ConnectorLocation[] {
                        ConnectorLocation.RIGHT,
                        ConnectorLocation.LEFT
                };
            }
            else {
                return new ConnectorLocation[] {
                        ConnectorLocation.LEFT,
                        ConnectorLocation.RIGHT
                };
            }
        }
        else {
            if (dy >= 0) {
                return new ConnectorLocation[] {
                        ConnectorLocation.BOTTOM,
                        ConnectorLocation.TOP
                };
            }
            else {
                return new ConnectorLocation[] {
                        ConnectorLocation.TOP,
                        ConnectorLocation.BOTTOM
                };
            }
        }
    }

    /**
     * 应用新的入口/出口，并重新计算 start / shapeStart / end。
     *
     * 注意：
     * 此方法接收的 paintedComponent 必须和构造函数里传入的是同一个坐标系组件。
     */
    protected void applyConnectorLocations(
            final ConnectorLocation newStartLocation,
            final ConnectorLocation newEndLocation,
            final Component paintedComponent
    ) {
        final MainView sourceMainView = source.getMainView();
        final MainView targetMainView = target.getMainView();

        start = getConnectorPoint(sourceMainView, newStartLocation);
        UITools.convertPointToAncestor(sourceMainView, start, paintedComponent);

        /*
         * 对动态接口来说，不沿用 folding mark 偏移。
         * 否则 shapeStart 可能仍带着旧方向的折叠标记补偿。
         */
        shapeStart = getConnectorPoint(sourceMainView, newStartLocation);
        UITools.convertPointToAncestor(sourceMainView, shapeStart, paintedComponent);

        end = getConnectorPoint(targetMainView, newEndLocation);
        UITools.convertPointToAncestor(targetMainView, end, paintedComponent);

        setStartConnectorLocation(newStartLocation);
        setEndConnectorLocation(newEndLocation);
    }

    protected Point getConnectorPoint(final MainView mainView, final ConnectorLocation location) {
        switch (location) {
            case LEFT:
                return mainView.getLeftPoint();
            case RIGHT:
                return mainView.getRightPoint();
            case TOP:
                return mainView.getTopPoint();
            case BOTTOM:
                return mainView.getBottomPoint();
            default:
                return mainView.getRightPoint();
        }
    }

    protected boolean isHorizontalConnectorLocation(final ConnectorLocation location) {
        return ConnectorLocation.LEFT.equals(location)
                || ConnectorLocation.RIGHT.equals(location);
    }

    protected Point getControlPoint(ConnectorLocation connectorLocation) {
        final int xctrl;
        final int yctrl;

        if (ConnectorLocation.LEFT.equals(connectorLocation)) {
            xctrl = -1;
            yctrl = 0;
        }
        else if (ConnectorLocation.RIGHT.equals(connectorLocation)) {
            xctrl = 1;
            yctrl = 0;
        }
        else if (ConnectorLocation.TOP.equals(connectorLocation)) {
            xctrl = 0;
            yctrl = -1;
        }
        else if (ConnectorLocation.BOTTOM.equals(connectorLocation)) {
            xctrl = 0;
            yctrl = 1;
        }
        else {
            xctrl = 0;
            yctrl = 0;
        }

        return new Point(xctrl, yctrl);
    }

    protected void align(Point start, Point end) {
        if (1 == Math.abs(start.y - end.y)) {
            end.y = start.y;
        }
    }

    public Color getColor(Graphics2D g) {
        Color color = getColor();

        if (getWidth() <= 0
                && g.getRenderingHint(RenderingHints.KEY_ANTIALIASING).equals(RenderingHints.VALUE_ANTIALIAS_OFF)) {
            int newAlpha = (color.getAlpha() & 0xFF) / 8;
            int newColor = (color.getRGB() & 0x00FFFFFF) | (newAlpha << 24);
            return new Color(newColor, true);
        }

        return color;
    }

    public Color getColor() {
        if (color == null) {
            if (highlightsAscendantEdge) {
                color = MapView.getHighlightAscendantEdgeColorRule().getValue();
            }
            else {
                color = target.getEdgeColor();
            }
        }

        return color;
    }

    public void setColor(final Color color) {
        this.color = color;
    }

    protected MapView getMap() {
        return getTarget().getMap();
    }

    public NodeView getSource() {
        return source;
    }

    protected Stroke getStroke() {
        final int width = getWidth();
        return getStroke(width);
    }

    protected Stroke getStroke(final float width) {
        int[] dash = getDash();

        if (width <= 0 && dash == null) {
            return EdgeView.DEF_STROKE;
        }

        final int[] dash1 = dash;
        return UITools.createStroke(width * getMap().getZoom(), dash1, BasicStroke.JOIN_ROUND);
    }

    public NodeView getTarget() {
        return target;
    }

    public int getWidth() {
        if (width != null) {
            return width;
        }

        width = target.getEdgeWidth();

        if (highlightsAscendantEdge) {
            width = width.intValue() + 2;
        }

        return width;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public int[] getDash() {
        if (dash != null) {
            return dash;
        }

        final Dash dash = target.getEdgeDash();
        return dash.pattern;
    }

    public void setDash(final int[] dash) {
        this.dash = dash;
    }

    protected boolean drawHiddenParentEdge() {
        return false;
    }

    abstract protected void draw(Graphics2D g);

    public void paint(final Graphics2D g) {
        final long start = PaintPerformanceMonitor.start();
        final Stroke stroke = g.getStroke();
        final Color color = g.getColor();
        try {
            g.setColor(getColor(g));
            g.setStroke(getStroke());
            draw(g);
        }
        finally {
            g.setStroke(stroke);
            g.setColor(color);
            PaintPerformanceMonitor.record(PaintPerformanceMonitor.EDGE, start);
        }
    }

    abstract public boolean detectCollision(Point p);
}
