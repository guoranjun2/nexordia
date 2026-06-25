package org.freeplane.core.ui.svgicons;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.UIManager;

import org.freeplane.core.util.ColorUtils;

class ThemedSVGIcon extends ImageIcon {
	private static final long serialVersionUID = 1L;
	private static final int MAX_CACHED_COLORS = 8;

	private final URL url;
	private final int heightPixels;
	private final int widthPixels;
	private final Map<String, ImageIcon> iconsByColor;
	private Dimension size;

	ThemedSVGIcon(URL url, int heightPixels, int widthPixels) {
		this.url = url;
		this.heightPixels = heightPixels;
		this.widthPixels = widthPixels;
		this.iconsByColor = new LinkedHashMap<String, ImageIcon>(MAX_CACHED_COLORS, 0.75f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) {
				return size() > MAX_CACHED_COLORS;
			}
		};
	}

	@Override
	public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
		iconFor(c).paintIcon(c, g, x, y);
	}

	@Override
	public synchronized int getIconWidth() {
		return size().width;
	}

	@Override
	public synchronized int getIconHeight() {
		return size().height;
	}

	@Override
	public synchronized Image getImage() {
		BufferedImage image = new BufferedImage(getIconWidth(), getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try {
			paintIcon(null, graphics, 0, 0);
		}
		finally {
			graphics.dispose();
		}
		return image;
	}

	private ImageIcon iconFor(Component component) {
		String replacements = replacementsFor(component);
		ImageIcon icon = iconsByColor.get(replacements);
		if (icon == null) {
			icon = createIcon(replacements);
			iconsByColor.put(replacements, icon);
		}
		return icon;
	}

	private ImageIcon createIcon(String replacements) {
		SVGIconCreator creator = new SVGIconCreator(url, replacements);
		if (heightPixels >= 0) {
			creator.setHeight(heightPixels);
		}
		if (widthPixels >= 0) {
			creator.setWidth(widthPixels);
		}
		return creator.createSvgIcon();
	}

	private Dimension size() {
		if (size == null) {
			ImageIcon icon = iconFor(null);
			size = new Dimension(icon.getIconWidth(), icon.getIconHeight());
		}
		return size;
	}

	private String replacementsFor(Component component) {
		Color foreground = foregroundFor(component);
		Color disabledForeground = disabledForeground();
		String primary = ColorUtils.colorToString(foreground);
		String secondary = ColorUtils.colorToString(disabledForeground);
		return "/#333\"/" + primary + "\"/#333;/" + primary + ";/#333333/" + primary
				+ "/#000\"/" + primary + "\"/#000;/" + primary + ";/#000000/" + primary
				+ "/#0071bc/" + primary + "/#0071BC/" + primary
				+ "/#999\"/" + secondary + "\"/#999;/" + secondary + ";/#999999/" + secondary;
	}

	private Color foregroundFor(Component component) {
		if (component != null && ! component.isEnabled()) {
			return disabledForeground();
		}
		if (component != null && component.getForeground() != null) {
			return component.getForeground();
		}
		Color color = UIManager.getColor("Button.foreground");
		if (color == null) {
			color = UIManager.getColor("Label.foreground");
		}
		return color != null ? color : Color.DARK_GRAY;
	}

	private Color disabledForeground() {
		Color color = UIManager.getColor("Button.disabledText");
		if (color == null) {
			color = UIManager.getColor("Label.disabledForeground");
		}
		if (color == null) {
			color = UIManager.getColor("Slider.disabledTrackColor");
		}
		return color != null ? color : new Color(0x8A8A8A);
	}
}
