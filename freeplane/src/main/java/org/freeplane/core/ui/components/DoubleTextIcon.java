package org.freeplane.core.ui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class DoubleTextIcon implements StyledIcon {
    private final TextIcon leftIcon;
    private final TextIcon rightIcon;
    private int paddingX;
    private int paddingY;
    private Color iconBackgroundColor;
    private Color underlineColor;
    private BasicStroke underlineStroke;
    private UnderlinePosition underlinePosition = UnderlinePosition.NONE;

    public DoubleTextIcon(String leftText, String rightText, FontMetrics fontMetrics) {
        this.leftIcon = new TextIcon(leftText, fontMetrics);
        this.rightIcon = new TextIcon(rightText, fontMetrics);
        this.leftIcon.setPaddingX(0);
        this.rightIcon.setPaddingX(0);
        this.leftIcon.setPaddingY(0);
        this.rightIcon.setPaddingY(0);
        this.leftIcon.setBorderType(TextIcon.BorderType.UNDERLINE);
        this.rightIcon.setBorderType(TextIcon.BorderType.UNDERLINE);
        updateUnderlineState();
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        int iconWidth = getIconWidth();
        int iconHeight = getIconHeight();
        if(iconBackgroundColor != null) {
            Color originalColor = graphics.getColor();
            graphics.setColor(iconBackgroundColor);
            graphics.fillRect(x, y, iconWidth, iconHeight);
            graphics.setColor(originalColor);
        }
        int contentX = x + paddingX;
        int contentY = y + paddingY;
        leftIcon.paintIcon(component, graphics, contentX, contentY);
        int rightIconX = contentX + leftIcon.getIconWidth();
        rightIcon.paintIcon(component, graphics, rightIconX, contentY);
    }

    @Override
    public int getIconWidth() {
        return paddingX * 2 + leftIcon.getIconWidth() + rightIcon.getIconWidth();
    }

    @Override
    public int getIconHeight() {
        int baseHeight = Math.max(leftIcon.getIconHeight(), rightIcon.getIconHeight());
        return paddingY * 2 + baseHeight;
    }

    @Override
    public Color getIconTextColor() {
        return leftIcon.getIconTextColor();
    }

    @Override
    public DoubleTextIcon setIconTextColor(Color iconTextColor) {
        leftIcon.setIconTextColor(iconTextColor);
        rightIcon.setIconTextColor(iconTextColor);
        return this;
    }

    @Override
    public Color getIconBackgroundColor() {
        return iconBackgroundColor;
    }

    @Override
    public DoubleTextIcon setIconBackgroundColor(Color iconBackgroundColor) {
        this.iconBackgroundColor = iconBackgroundColor;
        if(iconBackgroundColor != null) {
            leftIcon.setIconBackgroundColor(iconBackgroundColor);
            rightIcon.setIconBackgroundColor(iconBackgroundColor);
        }
        leftIcon.setIconBackgroundColor(null);
        rightIcon.setIconBackgroundColor(null);
        return this;
    }

    @Override
    public Color getIconBorderColor() {
        return underlineColor;
    }

    @Override
    public DoubleTextIcon setIconBorderColor(Color iconBorderColor) {
        this.underlineColor = iconBorderColor;
        if(iconBorderColor != null && underlineStroke == null) {
            underlineStroke = TextIcon.DEFAULT_STROKE;
        }
        updateUnderlineState();
        return this;
    }

    public TextIcon.BorderType getBorderType() {
        return TextIcon.BorderType.UNDERLINE;
    }

    @Override
    public BasicStroke getBorderStroke() {
        return underlineStroke;
    }

    @Override
    public DoubleTextIcon setBorderStroke(BasicStroke borderStroke) {
        this.underlineStroke = borderStroke;
        updateUnderlineState();
        return this;
    }

    @Override
    public int getPaddingX() {
        return paddingX;
    }

    @Override
    public void setPaddingX(int paddingX) {
        this.paddingX = paddingX;
    }

    @Override
    public int getPaddingY() {
        return paddingY;
    }

    @Override
    public void setPaddingY(int paddingY) {
        this.paddingY = paddingY;
    }

    @Override
    public void setPadding(int padding) {
        this.paddingX = padding;
        this.paddingY = padding;
    }

    public UnderlinePosition getUnderlinePosition() {
        return underlinePosition;
    }

    public DoubleTextIcon setUnderlinePosition(UnderlinePosition underlinePosition) {
        this.underlinePosition = underlinePosition;
        updateUnderlineState();
        return this;
    }

    public String getLeftText() {
        return leftIcon.getText();
    }

    public String getRightText() {
        return rightIcon.getText();
    }

    public int getRightIconX() {
        return paddingX + leftIcon.getIconWidth();
    }

    private void updateUnderlineState() {
        leftIcon.setIconBorderColor(underlineColor);
        rightIcon.setIconBorderColor(underlineColor);
        leftIcon.setBorderStroke(shouldPaintUnderlineLeft() ? underlineStroke : null);
        rightIcon.setBorderStroke(shouldPaintUnderlineRight() ? underlineStroke : null);
    }

    private boolean shouldPaintUnderlineLeft() {
        return underlinePosition == UnderlinePosition.LEFT || underlinePosition == UnderlinePosition.BOTH;
    }

    private boolean shouldPaintUnderlineRight() {
        return underlinePosition == UnderlinePosition.RIGHT || underlinePosition == UnderlinePosition.BOTH;
    }

    public enum UnderlinePosition {
        NONE,
        LEFT,
        RIGHT,
        BOTH
    }
}
