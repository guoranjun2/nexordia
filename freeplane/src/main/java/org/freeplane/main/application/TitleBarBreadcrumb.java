package org.freeplane.main.application;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.freeplane.core.util.TextUtils;
import org.freeplane.features.map.IMapChangeListener;
import org.freeplane.features.map.INodeSelectionListener;
import org.freeplane.features.map.IMapSelection;
import org.freeplane.features.map.INodeChangeListener;
import org.freeplane.features.map.MapChangeEvent;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.map.NodeChangeEvent;
import org.freeplane.features.map.NodeDeletionEvent;
import org.freeplane.features.map.NodeModel;
import org.freeplane.features.map.NodeMoveEvent;
import org.freeplane.features.mode.Controller;
import org.freeplane.features.mode.ModeController;
import org.freeplane.features.ui.IMapViewChangeListener;
import org.freeplane.view.swing.map.MainView;

import com.formdev.flatlaf.FlatClientProperties;

class TitleBarBreadcrumb extends JPanel implements IMapViewChangeListener, INodeSelectionListener, INodeChangeListener,
		IMapChangeListener {
	private static final long serialVersionUID = 1L;
	private static final int ICON_SIZE = 15;
	private static final int MINIMUM_NODE_PATH_TAIL_BUTTONS = 3;
	private static final int MAXIMUM_NODE_PATH_TAIL_BUTTONS = 10;
	private static final int NODE_PATH_TARGET_TEXT_LENGTH = 80;
	private static final int NODE_TEXT_MAXIMUM_LENGTH = 24;
	private static final int ELLIPSIS_NODE_PATH_INDEX = -1;
	private static final int BREADCRUMB_REFRESH_DELAY = 500;

	private String frameTitle;
	private ModeController listenerModeController;
	private final Timer refreshTimer;

	TitleBarBreadcrumb() {
		super(new FlowLayout(FlowLayout.LEFT, 0, 2));
		setOpaque(false);
		putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, Boolean.FALSE);
		refreshTimer = new Timer(BREADCRUMB_REFRESH_DELAY, e -> refresh());
		refreshTimer.setRepeats(false);
		refreshTimer.setCoalesce(true);
	}

	void install(Controller controller) {
		controller.getMapViewManager().addMapViewChangeListener(this);
		updateNodeSelectionListener();
		refresh();
	}

	void setTitle(String frameTitle) {
		this.frameTitle = frameTitle;
		refresh();
	}

	@Override
	public void afterViewChange(Component oldView, Component newView) {
		updateNodeSelectionListener();
		refresh();
	}

	@Override
	public void afterViewDisplayed(Component oldView, Component newView) {
		updateNodeSelectionListener();
		refresh();
	}

	@Override
	public void afterViewCreated(Component newView) {
		updateNodeSelectionListener();
		refresh();
	}

	@Override
	public void onSelect(NodeModel node) {
		refresh();
	}

	@Override
	public void onSelectionSetChange(IMapSelection selection) {
		refresh();
	}

	private void updateNodeSelectionListener() {
		ModeController currentModeController = Controller.getCurrentModeController();
		if (listenerModeController == currentModeController) {
			return;
		}
		if (listenerModeController != null) {
			listenerModeController.getMapController().removeNodeSelectionListener(this);
			listenerModeController.getMapController().removeNodeChangeListener(this);
			listenerModeController.getMapController().removeMapChangeListener(this);
		}
		listenerModeController = currentModeController;
		if (listenerModeController != null) {
			listenerModeController.getMapController().addNodeSelectionListener(this);
			listenerModeController.getMapController().addUINodeChangeListener(this);
			listenerModeController.getMapController().addUIMapChangeListener(this);
		}
	}

	@Override
	public void addNotify() {
		super.addNotify();
		updateNodeSelectionListener();
	}

	@Override
	public void removeNotify() {
		if (listenerModeController != null) {
			listenerModeController.getMapController().removeNodeSelectionListener(this);
			listenerModeController.getMapController().removeNodeChangeListener(this);
			listenerModeController.getMapController().removeMapChangeListener(this);
			listenerModeController = null;
		}
		refreshTimer.stop();
		super.removeNotify();
	}

	@Override
	public void nodeChanged(NodeChangeEvent event) {
		if (isCurrentMap(event.getNode().getMap())) {
			scheduleRefresh();
		}
	}

	@Override
	public void mapChanged(MapChangeEvent event) {
		if (event.getMap() == null || isCurrentMap(event.getMap())) {
			scheduleRefresh();
		}
	}

	@Override
	public void onNodeDeleted(NodeDeletionEvent nodeDeletionEvent) {
		if (isCurrentMap(nodeDeletionEvent.parent.getMap())) {
			scheduleRefresh();
		}
	}

	@Override
	public void onNodeInserted(NodeModel parent, NodeModel child, int newIndex) {
		if (isCurrentMap(parent.getMap())) {
			scheduleRefresh();
		}
	}

	@Override
	public void onNodeMoved(NodeMoveEvent nodeMoveEvent) {
		if (isCurrentMap(nodeMoveEvent.newParent.getMap()) || isCurrentMap(nodeMoveEvent.oldParent.getMap())) {
			scheduleRefresh();
		}
	}

	private void scheduleRefresh() {
		if (SwingUtilities.isEventDispatchThread()) {
			refreshTimer.restart();
		}
		else {
			SwingUtilities.invokeLater(() -> refreshTimer.restart());
		}
	}

	private boolean isCurrentMap(MapModel map) {
		Controller controller = Controller.getCurrentController();
		return controller != null && map != null && controller.getMap() == map;
	}

	private void refresh() {
		removeAll();
		File file = currentMapFile();
		if (file == null) {
			add(createFallbackLabel(frameTitle));
		}
		else {
			addFilePath(file.getAbsoluteFile(), frameTitle != null && frameTitle.trim().endsWith("*"));
			NodeModel selectedNode = currentSelectedNode();
			if (selectedNode != null) {
				add(createSectionSeparator());
				addNodePath(selectedNode);
			}
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

	private NodeModel currentSelectedNode() {
		Controller controller = Controller.getCurrentController();
		if (controller == null || controller.getSelection() == null) {
			return null;
		}
		NodeModel selected = controller.getSelection().getSelected();
		MapModel map = controller.getMap();
		return selected != null && selected.getMap() == map ? selected : null;
	}

	private void addFilePath(File file, boolean modified) {
		List<File> segments = new ArrayList<File>();
		for (File current = file; current != null; current = current.getParentFile()) {
			segments.add(0, current);
		}
		for (int i = 0; i < segments.size(); i++) {
			File segment = segments.get(i);
			boolean last = i == segments.size() - 1;
			add(new FileSegmentButton(segment, textFor(segment, last && modified), last));
			if (!last) {
				add(createPathSeparator());
			}
		}
		setToolTipText(file.getAbsolutePath());
	}

	private void addNodePath(NodeModel node) {
		NodeModel[] path = node.getPathToRoot();
		String[] pathTexts = nodeTexts(path);
		int[] visibleIndexes = visibleNodePathIndexes(pathTexts);
		for (int i = 0; i < visibleIndexes.length; i++) {
			if (i > 0) {
				add(createPathSeparator());
			}
			int pathIndex = visibleIndexes[i];
			if (pathIndex == ELLIPSIS_NODE_PATH_INDEX) {
				add(createEllipsisLabel());
			}
			else {
				NodeModel pathNode = path[pathIndex];
				add(new NodeSegmentButton(pathNode, pathTexts[pathIndex], pathIndex == path.length - 1));
			}
		}
		NodeModel firstChild = firstChild(node);
		if (firstChild != null) {
			add(createPathSeparator());
			add(new NodeSegmentButton(firstChild, nodeText(firstChild), false, true));
		}
	}

	static int[] visibleNodePathIndexes(String[] pathTexts) {
		int pathLength = pathTexts.length;
		if (pathLength <= 0) {
			return new int[0];
		}
		int tailButtonCount = nodePathTailButtonCount(pathTexts);
		int firstTailIndex = pathLength - tailButtonCount;
		if (firstTailIndex <= 1) {
			return fullNodePathIndexes(pathLength);
		}
		int[] indexes = new int[tailButtonCount + 2];
		indexes[0] = 0;
		indexes[1] = ELLIPSIS_NODE_PATH_INDEX;
		for (int i = 0; i < tailButtonCount; i++) {
			indexes[i + 2] = firstTailIndex + i;
		}
		return indexes;
	}

	private static int nodePathTailButtonCount(String[] pathTexts) {
		int textLength = 0;
		for (String text : pathTexts) {
			textLength += text.length();
		}
		double averageTextLength = Math.max(1d, textLength / (double) pathTexts.length);
		int buttonCount = (int) Math.round(NODE_PATH_TARGET_TEXT_LENGTH / averageTextLength);
		return clamp(buttonCount, MINIMUM_NODE_PATH_TAIL_BUTTONS, MAXIMUM_NODE_PATH_TAIL_BUTTONS);
	}

	private static int[] fullNodePathIndexes(int pathLength) {
		int[] indexes = new int[pathLength];
		for (int i = 0; i < pathLength; i++) {
			indexes[i] = i;
		}
		return indexes;
	}

	private static int clamp(int value, int minimum, int maximum) {
		return Math.max(minimum, Math.min(maximum, value));
	}

	private String textFor(File file, boolean modified) {
		String text = file.getName();
		if (text.length() == 0) {
			text = file.getPath();
		}
		return modified ? text + " *" : text;
	}

	private String nodeText(NodeModel node) {
		String text = TextUtils.getShortText(node.toString(), NODE_TEXT_MAXIMUM_LENGTH, "...");
		return text.trim().length() > 0 ? text : "  ";
	}

	private String[] nodeTexts(NodeModel[] nodes) {
		String[] texts = new String[nodes.length];
		for (int i = 0; i < nodes.length; i++) {
			texts[i] = nodeText(nodes[i]);
		}
		return texts;
	}

	private NodeModel firstChild(NodeModel node) {
		return node.getChildCount() > 0 ? node.getChildAt(0) : null;
	}

	private JLabel createPathSeparator() {
		return createSeparator(">");
	}

	private JLabel createSectionSeparator() {
		return createSeparator("|");
	}

	private JLabel createEllipsisLabel() {
		JLabel label = new JLabel("...");
		label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		label.setForeground(getForeground());
		label.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, Boolean.FALSE);
		return label;
	}

	private JLabel createSeparator(String text) {
		JLabel label = new JLabel(text);
		label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		label.setForeground(getForeground());
		label.putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, Boolean.FALSE);
		return label;
	}

	private void showSiblings(FileSegmentButton button, File file) {
		JPopupMenu popup = new JPopupMenu(file.getAbsolutePath());
		addFileItems(popup, siblingsOf(file));
		popup.show(button, 0, button.getHeight());
	}

	private void showSiblingNodes(NodeSegmentButton button, NodeModel node) {
		JPopupMenu popup = new JPopupMenu(node.toString());
		JMenuItem selectedItem = addNodeItems(popup, siblingNodes(node), node);
		popup.show(button, 0, button.getHeight());
		selectMenuItem(popup, selectedItem);
	}

	private void addFileItems(JPopupMenu popup, File[] files) {
		if (files.length == 0) {
			addEmptyItem(popup, "breadcrumb.no_items", "No items");
			return;
		}
		for (int i = 0; i < files.length && i < 250; i++) {
			popup.add(createFileMenuItem(files[i]));
		}
		if (files.length > 250) {
			addEmptyItem(popup, "breadcrumb.too_many_items", "More items omitted");
		}
	}

	private void addFileItems(JMenu menu, File[] files) {
		if (files.length == 0) {
			addEmptyItem(menu, "breadcrumb.no_items", "No items");
			return;
		}
		for (int i = 0; i < files.length && i < 250; i++) {
			menu.add(createFileMenuItem(files[i]));
		}
		if (files.length > 250) {
			addEmptyItem(menu, "breadcrumb.too_many_items", "More items omitted");
		}
	}

	private JMenuItem createFileMenuItem(File file) {
		if (file.isDirectory()) {
			return new LazyFileMenu(file);
		}
		JMenuItem item = new JMenuItem(fileName(file), new FileKindIcon(false));
		item.addActionListener(e -> openFile(file));
		return item;
	}

	private void addNodeItems(JPopupMenu popup, List<NodeModel> nodes) {
		addNodeItems(popup, nodes, null);
	}

	private JMenuItem addNodeItems(JPopupMenu popup, List<NodeModel> nodes, NodeModel selectedNode) {
		if (nodes.isEmpty()) {
			addEmptyItem(popup, "breadcrumb.node.no_items", "No nodes");
			return null;
		}
		JMenuItem selectedItem = null;
		for (NodeModel node : nodes) {
			JMenuItem item = createNodeMenuItem(node);
			popup.add(item);
			if (node == selectedNode) {
				selectedItem = item;
			}
		}
		return selectedItem;
	}

	private void addNodeItems(JMenu menu, List<NodeModel> nodes) {
		if (nodes.isEmpty()) {
			addEmptyItem(menu, "breadcrumb.node.no_items", "No nodes");
			return;
		}
		for (NodeModel node : nodes) {
			menu.add(createNodeMenuItem(node));
		}
	}

	private JMenuItem createNodeMenuItem(NodeModel node) {
		if (node.getChildCount() > 0) {
			return new LazyNodeMenu(node);
		}
		JMenuItem item = new JMenuItem(nodeText(node), renderedNodeIcon(node));
		item.addActionListener(e -> jumpToNode(node));
		return item;
	}

	private void addEmptyItem(JPopupMenu popup, String key, String fallback) {
		JMenuItem item = new JMenuItem(TextUtils.getText(key, fallback));
		item.setEnabled(false);
		popup.add(item);
	}

	private void addEmptyItem(JMenu menu, String key, String fallback) {
		JMenuItem item = new JMenuItem(TextUtils.getText(key, fallback));
		item.setEnabled(false);
		menu.add(item);
	}

	private List<NodeModel> siblingNodes(NodeModel node) {
		NodeModel parent = node.getParentNode();
		if (parent == null) {
			return node.getChildren();
		}
		return parent.getChildren();
	}

	private File[] siblingsOf(File file) {
		File parent = file.getParentFile();
		File[] files = parent != null ? parent.listFiles() : File.listRoots();
		return sortedFiles(files);
	}

	private File[] childrenOf(File file) {
		return sortedFiles(file.listFiles());
	}

	private File[] sortedFiles(File[] files) {
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

	private String fileName(File file) {
		return file.getName().length() == 0 ? file.getPath() : file.getName();
	}

	private void openFile(File file) {
		ApplicationFileActions.open(this, file);
	}

	private void jumpToNode(NodeModel node) {
		ApplicationNodeActions.jumpTo(node);
	}

	private void selectMenuItem(JPopupMenu popup, JMenuItem item) {
		if (item == null) {
			return;
		}
		SwingUtilities.invokeLater(() -> {
			if (popup.isVisible()) {
				MenuSelectionManager.defaultManager().setSelectedPath(new MenuElement[] { popup, item });
			}
		});
	}

	private void showFileMenu(Component component, int x, int y, File file) {
		JPopupMenu popup = new JPopupMenu(file.getAbsolutePath());
		ApplicationFileActions.addMenuItems(popup, this, file);
		popup.show(component, x, y);
	}

	private Icon renderedNodeIcon(NodeModel node) {
		Controller controller = Controller.getCurrentController();
		if (controller == null) {
			return null;
		}
		Component component = controller.getMapViewManager().getComponent(node);
		return component instanceof MainView ? ((MainView) component).getIcon() : null;
	}

	private void showNodeMenu(Component component, int x, int y, NodeModel node) {
		JPopupMenu popup = new JPopupMenu(node.toString());
		ApplicationNodeActions.addMenuItems(popup, this, node);
		popup.show(component, x, y);
	}

	@Override
	public void setForeground(Color foreground) {
		super.setForeground(softened(foreground));
		for (Component component : getComponents()) {
			if (component instanceof BreadcrumbButton) {
				((BreadcrumbButton) component).updateForeground();
			}
			else {
				component.setForeground(getForeground());
			}
		}
	}

	private Color softened(Color color) {
		if (color == null) {
			Color fallback = UIManager.getColor("Label.foreground");
			return fallback != null ? fallback : new Color(0x5F6368);
		}
		int max = Math.max(color.getRed(), Math.max(color.getGreen(), color.getBlue()));
		int min = Math.min(color.getRed(), Math.min(color.getGreen(), color.getBlue()));
		if (min > 245) {
			return new Color(0xD3D7DE);
		}
		if (max < 10) {
			return new Color(0x5F6368);
		}
		return color;
	}

	private Color mutedForeground() {
		Color disabledForeground = UIManager.getColor("Label.disabledForeground");
		return disabledForeground != null ? softened(disabledForeground) : new Color(0x8A8F98);
	}

	private abstract class BreadcrumbButton extends JButton {
		private static final long serialVersionUID = 1L;
		private final boolean muted;

		BreadcrumbButton(String text, Icon icon, boolean selected) {
			this(text, icon, selected, false);
		}

		BreadcrumbButton(String text, Icon icon, boolean selected, boolean muted) {
			super(text, icon);
			this.muted = muted;
			setOpaque(false);
			setFocusable(false);
			setFocusPainted(false);
			setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
			updateForeground();
			putClientProperty("JButton.buttonType", "toolBarButton");
			putClientProperty(FlatClientProperties.COMPONENT_TITLE_BAR_CAPTION, Boolean.FALSE);
			if (selected) {
				setFont(getFont().deriveFont(Font.BOLD));
			}
			setPreferredSize(new Dimension(getPreferredSize().width, 24));
		}

		void updateForeground() {
			setForeground(muted ? mutedForeground() : TitleBarBreadcrumb.this.getForeground());
		}
	}

	private class FileSegmentButton extends BreadcrumbButton {
		private static final long serialVersionUID = 1L;
		private final File file;
		private boolean popupShown;

		FileSegmentButton(File file, String text, boolean selected) {
			super(text, new FileKindIcon(file.isDirectory()), selected);
			this.file = file;
			setToolTipText(file.getAbsolutePath());
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
			if (event.getID() == MouseEvent.MOUSE_PRESSED) {
				popupShown = false;
			}
			if (event.isPopupTrigger()
					|| event.getID() == MouseEvent.MOUSE_RELEASED && SwingUtilities.isRightMouseButton(event)) {
				if (!popupShown) {
					popupShown = true;
					event.consume();
					showFileMenu(this, event.getX(), event.getY(), file);
				}
			}
		}
	}

	private class NodeSegmentButton extends BreadcrumbButton {
		private static final long serialVersionUID = 1L;
		private final NodeModel node;
		private boolean popupShown;

		NodeSegmentButton(NodeModel node, String text, boolean selected) {
			this(node, text, selected, false);
		}

		NodeSegmentButton(NodeModel node, String text, boolean selected, boolean muted) {
			super(text, renderedNodeIcon(node), selected, muted);
			this.node = node;
			setToolTipText(node.toString());
			addActionListener(e -> showSiblingNodes(this, node));
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
			if (event.getID() == MouseEvent.MOUSE_PRESSED) {
				popupShown = false;
			}
			if (event.isPopupTrigger()
					|| event.getID() == MouseEvent.MOUSE_RELEASED && SwingUtilities.isRightMouseButton(event)) {
				if (!popupShown) {
					popupShown = true;
					event.consume();
					showNodeMenu(this, event.getX(), event.getY(), node);
				}
			}
		}
	}

	private abstract class LazyBreadcrumbMenu extends JMenu {
		private static final long serialVersionUID = 1L;
		private boolean loaded;

		LazyBreadcrumbMenu(String text, Icon icon) {
			super(text);
			setIcon(icon);
			addEmptyItem(this, "breadcrumb.loading", "Loading...");
			addMenuListener(new MenuListener() {
				@Override
				public void menuSelected(MenuEvent event) {
					loadIfNeeded();
				}

				@Override
				public void menuDeselected(MenuEvent event) {
				}

				@Override
				public void menuCanceled(MenuEvent event) {
				}
			});
		}

		@Override
		public void processMouseEvent(MouseEvent event, MenuElement[] path, MenuSelectionManager manager) {
			if (event.getID() == MouseEvent.MOUSE_RELEASED && SwingUtilities.isLeftMouseButton(event)
					&& contains(event.getPoint())) {
				event.consume();
				manager.clearSelectedPath();
				runPrimaryAction();
				return;
			}
			super.processMouseEvent(event, path, manager);
		}

		private void loadIfNeeded() {
			if (loaded) {
				return;
			}
			loaded = true;
			removeAll();
			loadChildren();
		}

		abstract void loadChildren();

		abstract void runPrimaryAction();
	}

	private class LazyFileMenu extends LazyBreadcrumbMenu {
		private static final long serialVersionUID = 1L;
		private final File file;

		LazyFileMenu(File file) {
			super(fileName(file), new FileKindIcon(true));
			this.file = file;
		}

		@Override
		void loadChildren() {
			addFileItems(this, childrenOf(file));
		}

		@Override
		void runPrimaryAction() {
			openFile(file);
		}
	}

	private class LazyNodeMenu extends LazyBreadcrumbMenu {
		private static final long serialVersionUID = 1L;
		private final NodeModel node;

		LazyNodeMenu(NodeModel node) {
			super(nodeText(node), renderedNodeIcon(node));
			this.node = node;
		}

		@Override
		void loadChildren() {
			addNodeItems(this, node.getChildren());
		}

		@Override
		void runPrimaryAction() {
			jumpToNode(node);
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
					Shape folderShape = folderShape(x, y);
					g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.16f));
					g.fill(folderShape);
					g.setComposite(AlphaComposite.SrcOver);
					g.draw(folderShape);
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

		private Shape folderShape(int x, int y) {
			Path2D path = new Path2D.Float();
			path.moveTo(x + 1, y + 4);
			path.lineTo(x + 5, y + 4);
			path.lineTo(x + 6.5f, y + 6);
			path.lineTo(x + 14, y + 6);
			path.lineTo(x + 14, y + 13);
			path.lineTo(x + 1, y + 13);
			path.closePath();
			return path;
		}
	}

}
