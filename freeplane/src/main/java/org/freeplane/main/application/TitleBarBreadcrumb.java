package org.freeplane.main.application;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.Controller;

import com.formdev.flatlaf.FlatClientProperties;

class TitleBarBreadcrumb extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int ICON_SIZE = 15;

	TitleBarBreadcrumb() {
		super(new FlowLayout(FlowLayout.LEFT, 0, 2));
		setOpaque(false);
		putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, Boolean.FALSE);
	}

	void setTitle(String frameTitle) {
		removeAll();
		File file = currentMapFile();
		if (file == null) {
			add(createFallbackLabel(frameTitle));
		}
		else {
			addPath(file.getAbsoluteFile(), frameTitle != null && frameTitle.trim().endsWith("*"));
		}
		revalidate();
		repaint();
	}

	private JLabel createFallbackLabel(String frameTitle) {
		JLabel label = new JLabel(frameTitle != null ? frameTitle : "");
		label.setForeground(getForeground());
		label.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, Boolean.FALSE);
		return label;
	}

	private File currentMapFile() {
		Controller controller = Controller.getCurrentController();
		if (controller == null) {
			return null;
		}
		MapModel map = controller.getMap();
		return map != null ? map.getFile() : null;
	}

	private void addPath(File file, boolean modified) {
		List<File> segments = new ArrayList<File>();
		for (File current = file; current != null; current = current.getParentFile()) {
			segments.add(0, current);
		}
		for (int i = 0; i < segments.size(); i++) {
			File segment = segments.get(i);
			boolean last = i == segments.size() - 1;
			add(new SegmentButton(segment, textFor(segment, last && modified), last));
			if (!last) {
				add(createSeparator());
			}
		}
		setToolTipText(file.getAbsolutePath());
	}

	private String textFor(File file, boolean modified) {
		String text = file.getName();
		if (text.length() == 0) {
			text = file.getPath();
		}
		return modified ? text + " *" : text;
	}

	private JLabel createSeparator() {
		JLabel label = new JLabel(">");
		label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		label.setForeground(getForeground());
		label.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, Boolean.FALSE);
		return label;
	}

	private void showSiblings(SegmentButton button, File file) {
		File[] siblings = siblingsOf(file);
		JPopupMenu popup = new JPopupMenu(file.getAbsolutePath());
		if (siblings.length == 0) {
			JMenuItem empty = new JMenuItem(TextUtils.getText("breadcrumb.no_items", "No items"));
			empty.setEnabled(false);
			popup.add(empty);
		}
		else {
			for (int i = 0; i < siblings.length && i < 250; i++) {
				File sibling = siblings[i];
				JMenuItem item = new JMenuItem(sibling.getName().length() == 0 ? sibling.getPath() : sibling.getName(),
						new FileKindIcon(sibling.isDirectory()));
				item.addActionListener(e -> ApplicationFileActions.open(this, sibling));
				popup.add(item);
			}
			if (siblings.length > 250) {
				JMenuItem more = new JMenuItem(TextUtils.getText("breadcrumb.too_many_items", "More items omitted"));
				more.setEnabled(false);
				popup.add(more);
			}
		}
		popup.show(button, 0, button.getHeight());
	}

	private File[] siblingsOf(File file) {
		File parent = file.getParentFile();
		File[] files = parent != null ? parent.listFiles() : File.listRoots();
		if (files == null) {
			return new File[0];
		}
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File first, File second) {
				if (first.isDirectory() != second.isDirectory()) {
					return first.isDirectory() ? -1 : 1;
				}
				return first.getName().compareToIgnoreCase(second.getName());
			}
		});
		return files;
	}

	private void showFileMenu(Component component, int x, int y, File file) {
		JPopupMenu popup = new JPopupMenu(file.getAbsolutePath());
		ApplicationFileActions.addMenuItems(popup, this, file);
		popup.show(component, x, y);
	}

	@Override
	public void setForeground(Color foreground) {
		super.setForeground(foreground);
		for (Component component : getComponents()) {
			component.setForeground(foreground);
		}
	}

	private class SegmentButton extends JButton {
		private static final long serialVersionUID = 1L;
		private final File file;

		SegmentButton(File file, String text, boolean selected) {
			super(text, new FileKindIcon(file.isDirectory()));
			this.file = file;
			setOpaque(false);
			setFocusable(false);
			setFocusPainted(false);
			setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
			setForeground(TitleBarBreadcrumb.this.getForeground());
			setToolTipText(file.getAbsolutePath());
			putClientProperty("JButton.buttonType", "toolBarButton");
			putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, Boolean.FALSE);
			if (selected) {
				setFont(getFont().deriveFont(Font.BOLD));
			}
			setPreferredSize(new Dimension(getPreferredSize().width, 24));
			addActionListener(e -> showSiblings(this, file));
			addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent event) {
					maybeShowPopup(event);
				}

				@Override
				public void mouseReleased(MouseEvent event) {
					maybeShowPopup(event);
				}
			});
		}

		private void maybeShowPopup(MouseEvent event) {
			if (event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event)) {
				showFileMenu(this, event.getX(), event.getY(), file);
			}
		}
	}

	private static class FileKindIcon implements Icon {
		private final boolean folder;

		FileKindIcon(boolean folder) {
			this.folder = folder;
		}

		@Override
		public int getIconWidth() {
			return ICON_SIZE;
		}

		@Override
		public int getIconHeight() {
			return ICON_SIZE;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D g = (Graphics2D) graphics.create();
			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g.setColor(component.getForeground());
				if (folder) {
					g.drawRoundRect(x + 1, y + 5, 13, 8, 3, 3);
					g.drawLine(x + 2, y + 5, x + 5, y + 2);
					g.drawLine(x + 5, y + 2, x + 9, y + 2);
					g.drawLine(x + 9, y + 2, x + 11, y + 5);
				}
				else {
					g.drawRoundRect(x + 3, y + 1, 9, 13, 2, 2);
					g.drawLine(x + 9, y + 1, x + 12, y + 4);
					g.drawLine(x + 9, y + 1, x + 9, y + 4);
					g.drawLine(x + 9, y + 4, x + 12, y + 4);
				}
			}
			finally {
				g.dispose();
			}
		}
	}
}
