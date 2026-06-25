package org.freeplane.features.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;

import org.freeplane.core.resources.ResourceController;
import org.freeplane.core.ui.components.resizer.UIComponentVisibilityDispatcher;
import org.freeplane.core.util.TextUtils;
import org.freeplane.features.ui.toolwindow.ToolWindowAnchor;
import org.freeplane.features.ui.toolwindow.ToolWindowLayoutModel;
import org.freeplane.features.ui.toolwindow.ToolWindowMode;
import org.freeplane.features.ui.toolwindow.ToolWindowState;

class ModernToolWindowPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final String PANEL_PROPERTY = ModernToolWindowPanel.class.getName() + ".panel";
	private static final String PROPERTY_PREFIX = "modern_tool_window.";
	private static final Dimension STRIPE_BUTTON_SIZE = new Dimension(32, 32);
	private static final Dimension LAYOUT_PLACEHOLDER_SIZE = new Dimension(160, 90);
	private static final int TOP_REGION = 0;
	private static final int BOTTOM_REGION = 1;
	private static final int RESIZE_HANDLE_SIZE = 5;
	private static final int MINIMUM_PANEL_SIZE = 56;
	private static final int DEFAULT_PANEL_WIDTH = 280;
	private static final int DEFAULT_PANEL_HEIGHT = 220;
	private static final List<ModernToolWindowPanel> PANELS = new ArrayList<ModernToolWindowPanel>();

	private final ToolWindowAnchor anchor;
	private final ToolWindowLayoutModel model;
	private final JPanel stripe;
	private final JPanel topTabs;
	private final JPanel content;
	private final JPanel tabPlaceholder;
	private final JPanel tabSeparator;
	private final JPanel layoutPlaceholder;
	private final JPanel resizeHandle;
	private final JToggleButton moreButton;
	private final Map<Component, ToolWindow> windowsByComponent;
	private final Map<String, ToolWindow> windowsById;
	private final List<ModernToolWindowPanel> peers;
	private boolean rebuilding;
	private boolean collapsed;
	private int placeholderRegion;
	private int preferredPanelSize;

	ModernToolWindowPanel(ToolWindowAnchor anchor) {
		super(new BorderLayout(0, 0));
		this.anchor = anchor;
		model = new ToolWindowLayoutModel();
		stripe = new JPanel(new BorderLayout(0, 0));
		stripe.setBorder(stripeBorder());
		stripe.setBackground(UIManager.getColor("Panel.background"));
		topTabs = createTabGroup();
		stripe.add(topTabs, primaryStripePosition());
		content = new JPanel(new BorderLayout(0, 0));
		content.setOpaque(false);
		tabPlaceholder = createTabPlaceholder();
		tabSeparator = createTabSeparator();
		layoutPlaceholder = createLayoutPlaceholder();
		moreButton = createMoreButton();
		placeholderRegion = ToolWindowLayoutModel.DEFAULT_REGION;
		resizeHandle = createResizeHandle();
		windowsByComponent = new IdentityHashMap<Component, ToolWindow>();
		windowsById = new LinkedHashMap<String, ToolWindow>();
		peers = new ArrayList<ModernToolWindowPanel>();
		if (anchor == ToolWindowAnchor.BOTTOM) {
			add(resizeHandle, BorderLayout.NORTH);
			add(stripe, BorderLayout.SOUTH);
		}
		else {
			add(stripe, anchor == ToolWindowAnchor.LEFT ? BorderLayout.WEST : BorderLayout.EAST);
			add(resizeHandle, anchor == ToolWindowAnchor.LEFT ? BorderLayout.EAST : BorderLayout.WEST);
		}
		add(content, BorderLayout.CENTER);
		preferredPanelSize = restoredPreferredSize();
		updatePreferredSize(false, false);
		rebuildTabs();
	}

	void setPeer(ModernToolWindowPanel peer) {
		peers.clear();
		if (peer != null) {
			peers.add(peer);
		}
	}

	void setPeers(ModernToolWindowPanel... panels) {
		peers.clear();
		for (ModernToolWindowPanel panel : panels) {
			if (panel != null) {
				peers.add(panel);
			}
		}
	}

	static boolean canManage(JComponent component) {
		String id = toolbarId(component);
		return "/icon_toolbar".equals(id) || "/format".equals(id) || "/filter_toolbar".equals(id);
	}

	static ModernToolWindowPanel of(JComponent component) {
		return (ModernToolWindowPanel) component.getClientProperty(PANEL_PROPERTY);
	}

	@Override
	public void addNotify() {
		super.addNotify();
		if (!PANELS.contains(this)) {
			PANELS.add(this);
		}
	}

	@Override
	public void removeNotify() {
		PANELS.remove(this);
		super.removeNotify();
	}

	void addToolWindow(JComponent component, int index) {
		addToolWindow(component, index, null);
	}

	void addToolWindow(String id, JComponent component, int index, String visiblePropertyBaseName) {
		component.putClientProperty(ToolWindowClientProperties.TOOLBAR_ID, id);
		if (visiblePropertyBaseName != null && UIComponentVisibilityDispatcher.of(component) == null) {
			UIComponentVisibilityDispatcher.install(component, visiblePropertyBaseName);
		}
		addToolWindow(component, index, visiblePropertyBaseName);
	}

	void removeToolWindow(Component component) {
		ToolWindow window = windowsByComponent.remove(component);
		if (window == null) {
			content.remove(component);
			return;
		}
		disposeFloatingFrame(window);
		windowsById.remove(window.id);
		model.unregister(window.id);
		window.component.putClientProperty(PANEL_PROPERTY, null);
		rebuildTabs();
		rebuildDockedContent();
		revalidate();
		repaint();
	}

	void toggle(JComponent component) {
		ToolWindow window = windowsByComponent.get(component);
		if (window != null) {
			activate(window);
		}
	}

	boolean isToolWindowVisible(JComponent component) {
		ToolWindow window = windowsByComponent.get(component);
		if (window == null) {
			return component.isVisible();
		}
		return model.state(window.id).isVisible();
	}

	boolean isToolWindowVisible(String id) {
		ToolWindow window = windowsById.get(id);
		return window != null && model.state(id).isVisible();
	}

	void setToolWindowVisible(String id, boolean visible) {
		ToolWindow window = windowsById.get(id);
		if (window == null) {
			return;
		}
		if (visible) {
			showWindow(window);
		}
		else {
			hideWindow(window);
		}
	}

	void dockToolWindow(String id, ToolWindowAnchor newAnchor) {
		ToolWindow window = windowsById.get(id);
		if (window == null) {
			return;
		}
		ModernToolWindowPanel target = panelForAnchor(newAnchor);
		if (target == null || target == this) {
			dockWindow(window);
		}
		else {
			transferTo(window, target, model.state(window.id).region());
		}
	}

	private ModernToolWindowPanel panelForAnchor(ToolWindowAnchor targetAnchor) {
		if (targetAnchor == anchor) {
			return this;
		}
		for (ModernToolWindowPanel peerPanel : peers) {
			if (peerPanel.anchor == targetAnchor) {
				return peerPanel;
			}
		}
		for (ModernToolWindowPanel panel : new ArrayList<ModernToolWindowPanel>(PANELS)) {
			if (panel.anchor == targetAnchor) {
				return panel;
			}
		}
		return null;
	}

	private void addToolWindow(JComponent component, int index, String visiblePropertyBaseName) {
		String id = toolbarId(component);
		if (id == null) {
			return;
		}
		removeToolWindow(component);
		ToolWindowAnchor restoredAnchor = restoredAnchor(id, anchor);
		ModernToolWindowPanel restoredPanel = panelForAnchor(restoredAnchor);
		if (restoredPanel != null && restoredPanel != this) {
			restoredPanel.addToolWindow(component, index, visiblePropertyBaseName);
			return;
		}
		String title = titleFor(id);
		UIComponentVisibilityDispatcher dispatcher = UIComponentVisibilityDispatcher.of(component);
		ToolWindowMode mode = restoredMode(id, dispatcher == null || dispatcher.isVisible());
		int region = restoredRegion(id);
		ToolWindowState state = model.register(id, anchor, mode, region);
		ToolWindow window = new ToolWindow(id, title, component, createButton(id, title), index);
		windowsByComponent.put(component, window);
		windowsById.put(id, window);
		component.putClientProperty(PANEL_PROPERTY, this);
		saveStates();
		applyStates();
		rebuildTabs();
		if (state.mode() == ToolWindowMode.FLOATING) {
			SwingUtilities.invokeLater(() -> floatWindow(window, null));
		}
		else {
			rebuildDockedContent();
		}
	}

	private JToggleButton createButton(String id, String title) {
		JToggleButton button = new JToggleButton(iconFor(id));
		button.setFocusable(false);
		button.setFocusPainted(false);
		button.setPreferredSize(STRIPE_BUTTON_SIZE);
		button.setMinimumSize(STRIPE_BUTTON_SIZE);
		button.setMaximumSize(STRIPE_BUTTON_SIZE);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setToolTipText(title);
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		DragHandler dragHandler = new DragHandler(id);
		button.addActionListener(e -> {
			if (dragHandler.consumeActionSuppression()) {
				return;
			}
			activate(windowsById.get(id));
		});
		button.addMouseListener(dragHandler);
		button.addMouseMotionListener(dragHandler);
		return button;
	}

	private JPanel createTabGroup() {
		JPanel group = new JPanel();
		group.setLayout(new BoxLayout(group, anchor == ToolWindowAnchor.BOTTOM ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS));
		group.setAlignmentX(Component.LEFT_ALIGNMENT);
		group.setOpaque(false);
		return group;
	}

	private EmptyBorder stripeBorder() {
		return anchor == ToolWindowAnchor.BOTTOM ? new EmptyBorder(3, 4, 3, 4) : new EmptyBorder(4, 3, 4, 3);
	}

	private String primaryStripePosition() {
		return anchor == ToolWindowAnchor.BOTTOM ? BorderLayout.WEST : BorderLayout.NORTH;
	}

	private JToggleButton createMoreButton() {
		JToggleButton button = new JToggleButton(new ToolWindowIcon(ToolWindowIcon.Kind.MORE));
		button.setFocusable(false);
		button.setFocusPainted(false);
		button.setPreferredSize(STRIPE_BUTTON_SIZE);
		button.setMinimumSize(STRIPE_BUTTON_SIZE);
		button.setMaximumSize(STRIPE_BUTTON_SIZE);
		button.setAlignmentX(Component.LEFT_ALIGNMENT);
		button.setToolTipText(TextUtils.getText("tool_window_more", "More"));
		button.putClientProperty("JButton.buttonType", "toolBarButton");
		button.addActionListener(e -> {
			JPopupMenu popup = createOverflowPopup();
			popup.show(button, 0, button.getHeight());
			button.setSelected(false);
		});
		return button;
	}

	private JPanel createTabPlaceholder() {
		JPanel component = new JPanel();
		component.setOpaque(false);
		component.setPreferredSize(STRIPE_BUTTON_SIZE);
		component.setMinimumSize(STRIPE_BUTTON_SIZE);
		component.setMaximumSize(STRIPE_BUTTON_SIZE);
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		component.setBorder(new DashedBorder());
		component.setVisible(false);
		return component;
	}

	private JPanel createTabSeparator() {
		JPanel component = new TabSeparator(anchor);
		Dimension size = anchor == ToolWindowAnchor.BOTTOM ? new Dimension(9, STRIPE_BUTTON_SIZE.height)
				: new Dimension(STRIPE_BUTTON_SIZE.width, 9);
		component.setPreferredSize(size);
		component.setMinimumSize(size);
		component.setMaximumSize(size);
		component.setAlignmentX(Component.LEFT_ALIGNMENT);
		return component;
	}

	private JPanel createLayoutPlaceholder() {
		JPanel component = new LayoutPreviewPanel();
		component.setPreferredSize(LAYOUT_PLACEHOLDER_SIZE);
		component.setMinimumSize(LAYOUT_PLACEHOLDER_SIZE);
		component.setBorder(new DashedBorder());
		component.setVisible(false);
		return component;
	}

	private void rebuildTabs() {
		topTabs.removeAll();
		addRegionTabs(TOP_REGION);
		if (hasRegionTabs(TOP_REGION) && hasRegionTabs(BOTTOM_REGION)) {
			topTabs.add(tabSeparator);
		}
		addRegionTabs(BOTTOM_REGION);
		if (isOverflowHost()) {
			topTabs.add(moreButton);
		}
		setVisible(shouldShowPanel(hasDockedWindow(), layoutPlaceholder.isVisible()));
		stripe.revalidate();
		stripe.repaint();
	}

	private void addRegionTabs(int region) {
		if (tabPlaceholder.isVisible() && placeholderRegion == region) {
			topTabs.add(tabPlaceholder);
		}
		for (ToolWindow window : windowsById.values()) {
			ToolWindowState state = model.state(window.id);
			if (state.mode() == ToolWindowMode.HIDDEN || state.region() != region) {
				continue;
			}
			topTabs.add(window.button);
		}
	}

	private boolean hasRegionTabs(int region) {
		if (tabPlaceholder.isVisible() && placeholderRegion == region) {
			return true;
		}
		for (ToolWindow window : windowsById.values()) {
			ToolWindowState state = model.state(window.id);
			if (state.mode() != ToolWindowMode.HIDDEN && state.region() == region) {
				return true;
			}
		}
		return false;
	}

	private JPopupMenu createPopup(ToolWindow window) {
		JPopupMenu menu = new JPopupMenu(window.title);
		ToolWindowState state = model.state(window.id);
		JMenuItem hide = new JMenuItem(TextUtils.getText("tool_window_hide", "Hide"));
		hide.setEnabled(state.mode() != ToolWindowMode.HIDDEN);
		hide.addActionListener(e -> hideWindow(window));
		menu.add(hide);
		menu.add(createMoveMenu(window));
		JMenuItem floating = new JMenuItem(TextUtils.getText("tool_window_float", "Float"));
		floating.setEnabled(state.mode() != ToolWindowMode.FLOATING);
		floating.addActionListener(e -> floatWindow(window, null));
		menu.add(floating);
		JMenuItem dock = new JMenuItem(TextUtils.getText("tool_window_dock", "Dock"));
		dock.setEnabled(state.mode() == ToolWindowMode.FLOATING);
		dock.addActionListener(e -> dockWindow(window));
		menu.add(dock);
		return menu;
	}

	private JPopupMenu createOverflowPopup() {
		JPopupMenu menu = new JPopupMenu(TextUtils.getText("tool_window_more", "More"));
		for (ModernToolWindowPanel panel : new ArrayList<ModernToolWindowPanel>(PANELS)) {
			for (ToolWindow window : panel.windowsById.values()) {
				if (panel.model.state(window.id).mode() != ToolWindowMode.HIDDEN) {
					continue;
				}
				JMenuItem item = new JMenuItem(window.title, iconFor(window.id));
				item.addActionListener(e -> panel.showWindowFromOverflow(window));
				menu.add(item);
			}
		}
		return menu;
	}

	private JMenu createMoveMenu(ToolWindow window) {
		JMenu menu = new JMenu(TextUtils.getText("tool_window_move", "Move"));
		addMoveItem(menu, window, ToolWindowAnchor.LEFT, TOP_REGION,
				TextUtils.getText("tool_window_move_left_top", "Left Top"));
		addMoveItem(menu, window, ToolWindowAnchor.LEFT, BOTTOM_REGION,
				TextUtils.getText("tool_window_move_left_bottom", "Left Bottom"));
		addMoveItem(menu, window, ToolWindowAnchor.RIGHT, TOP_REGION,
				TextUtils.getText("tool_window_move_right_top", "Right Top"));
		addMoveItem(menu, window, ToolWindowAnchor.RIGHT, BOTTOM_REGION,
				TextUtils.getText("tool_window_move_right_bottom", "Right Bottom"));
		return menu;
	}

	private void addMoveItem(JMenu menu, ToolWindow window, ToolWindowAnchor targetAnchor, int region, String title) {
		JMenuItem item = new JMenuItem(title);
		item.addActionListener(e -> moveWindow(window, targetAnchor, region));
		menu.add(item);
	}

	private void activate(ToolWindow window) {
		if (window == null) {
			return;
		}
		ToolWindowState state = model.activate(window.id);
		if (state.mode() == ToolWindowMode.FLOATING) {
			focusFloatingWindow(window);
		}
		else if (state.mode() == ToolWindowMode.DOCKED) {
			showWindow(window);
		}
		else if (state.mode() == ToolWindowMode.INACTIVE) {
			deactivateWindow(window);
		}
		else {
			hideWindow(window);
		}
	}

	private void showWindow(ToolWindow window) {
		dockComponentIntoPanel(window);
		model.show(window.id);
		setCollapsed(false);
		saveStates();
		applyStates();
		rebuildTabs();
		rebuildDockedContent();
	}

	private void showWindowFromOverflow(ToolWindow window) {
		moveHiddenWindowToOpenRegion(window);
		showWindow(window);
	}

	private void deactivateWindow(ToolWindow window) {
		saveState(window);
		applyStates();
		rebuildTabs();
		rebuildDockedContent();
	}

	private void moveHiddenWindowToOpenRegion(ToolWindow window) {
		ToolWindowState state = model.state(window.id);
		if (state.mode() != ToolWindowMode.HIDDEN) {
			return;
		}
		int region = state.region();
		if (dockedWindowInRegion(region) != null && dockedWindowInRegion(oppositeRegion(region)) == null) {
			model.moveHiddenToRegion(window.id, oppositeRegion(region));
		}
	}

	private void hideWindow(ToolWindow window) {
		dockComponentIntoPanel(window);
		model.hide(window.id);
		saveState(window);
		applyStates();
		rebuildTabs();
		rebuildDockedContent();
	}

	private void floatWindow(ToolWindow window, Point screenPoint) {
		if (window.floatingFrame == null) {
			UIComponentVisibilityDispatcher dispatcher = UIComponentVisibilityDispatcher.of(window.component);
			if (dispatcher != null) {
				dispatcher.setVisible(true);
			}
			else {
				window.component.setVisible(true);
			}
			removeFromParent(window.component);
			JFrame frame = new JFrame(window.title);
			frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent event) {
					dockWindow(window);
				}
			});
			frame.getContentPane().setLayout(new BorderLayout());
			frame.getContentPane().add(window.component, BorderLayout.CENTER);
			window.floatingFrame = frame;
			model.floatWindow(window.id);
			frame.pack();
			positionFloatingFrame(window, screenPoint);
			frame.setVisible(true);
		}
		else {
			positionFloatingFrame(window, screenPoint);
		}
		saveState(window);
		applyStates();
		rebuildTabs();
		rebuildDockedContent();
	}

	private void moveWindow(ToolWindow window, ToolWindowAnchor targetAnchor, int region) {
		ModernToolWindowPanel target = panelForAnchor(targetAnchor);
		if (target == null || target == this) {
			dockWindowInRegion(window, region);
		}
		else {
			transferTo(window, target, region);
		}
	}

	private void positionFloatingFrame(ToolWindow window, Point screenPoint) {
		if (window.floatingFrame == null) {
			return;
		}
		if (screenPoint != null) {
			window.floatingFrame.setLocation(screenPoint.x - 18, screenPoint.y - 18);
			return;
		}
		Window owner = SwingUtilities.getWindowAncestor(this);
		if (owner != null) {
			window.floatingFrame.setLocationRelativeTo(owner);
		}
	}

	private void dockWindow(ToolWindow window) {
		dockComponentIntoPanel(window);
		model.dock(window.id, anchor);
		setCollapsed(false);
		saveStates();
		applyStates();
		rebuildTabs();
		rebuildDockedContent();
	}

	private void dockWindowInRegion(ToolWindow window, int region) {
		dockComponentIntoPanel(window);
		makeRoomForDrop(window.id, region);
		model.moveToRegion(window.id, region);
		setCollapsed(false);
		saveStates();
		applyStates();
		rebuildTabs();
		rebuildDockedContent();
	}

	private void dockComponentIntoPanel(ToolWindow window) {
		disposeFloatingFrame(window);
		removeFromParent(window.component);
	}

	private void transferTo(ToolWindow window, ModernToolWindowPanel target, int region) {
		disposeFloatingFrame(window);
		removeFromParent(window.component);
		windowsByComponent.remove(window.component);
		windowsById.remove(window.id);
		window.component.putClientProperty(PANEL_PROPERTY, target);
		model.unregister(window.id);
		rebuildTabs();
		target.makeRoomForDrop(window.id, region);
		target.receive(window, region);
		rebuildDockedContent();
		revalidate();
		repaint();
	}

	private void receive(ToolWindow window, int region) {
		ToolWindow received = new ToolWindow(window.id, window.title, window.component,
				createButton(window.id, window.title), window.index);
		windowsByComponent.put(received.component, received);
		windowsById.put(received.id, received);
		model.register(received.id, anchor, ToolWindowMode.DOCKED, region);
		setCollapsed(false);
		saveStates();
		applyStates();
		rebuildTabs();
		rebuildDockedContent();
		revalidate();
		repaint();
	}

	private void disposeFloatingFrame(ToolWindow window) {
		if (window.floatingFrame == null) {
			return;
		}
		window.floatingFrame.getContentPane().remove(window.component);
		window.floatingFrame.dispose();
		window.floatingFrame = null;
	}

	private void focusFloatingWindow(ToolWindow window) {
		if (window.floatingFrame != null) {
			window.floatingFrame.toFront();
			window.floatingFrame.requestFocus();
		}
	}

	private void applyState(ToolWindow window) {
		ToolWindowState state = model.state(window.id);
		window.button.setSelected(state.isVisible());
		if (state.mode() != ToolWindowMode.FLOATING) {
			UIComponentVisibilityDispatcher dispatcher = UIComponentVisibilityDispatcher.of(window.component);
			if (dispatcher != null) {
				dispatcher.setVisible(state.mode() == ToolWindowMode.DOCKED);
			}
			else {
				window.component.setVisible(state.mode() == ToolWindowMode.DOCKED);
			}
		}
	}

	private void applyStates() {
		for (ToolWindow window : windowsById.values()) {
			applyState(window);
		}
	}

	private void rebuildDockedContent() {
		if (rebuilding) {
			return;
		}
		rebuilding = true;
		try {
			content.removeAll();
			boolean hasPreview = layoutPlaceholder.isVisible();
			boolean hasDockedWindow = hasDockedWindow();
			JComponent topComponent = componentForRegion(TOP_REGION);
			JComponent bottomComponent = componentForRegion(BOTTOM_REGION);
			if (hasPreview) {
				if (topComponent == null) {
					topComponent = createEmptyLayoutRegion();
				}
				if (bottomComponent == null) {
					bottomComponent = createEmptyLayoutRegion();
				}
				content.add(createPreviewSplit(topComponent, bottomComponent), BorderLayout.CENTER);
			}
			else if (topComponent != null && bottomComponent != null) {
				content.add(createRegionalSplit(topComponent, bottomComponent), BorderLayout.CENTER);
			}
			else if (topComponent != null) {
				content.add(topComponent, BorderLayout.CENTER);
			}
			else if (bottomComponent != null) {
				content.add(bottomComponent, BorderLayout.CENTER);
			}
			updatePreferredSize(hasDockedWindow, hasPreview);
			content.revalidate();
			content.repaint();
			revalidate();
			repaint();
		}
		finally {
			rebuilding = false;
		}
	}

	private JComponent componentForRegion(int region) {
		if (layoutPlaceholder.isVisible() && placeholderRegion == region) {
			return createLayoutPlaceholderContainer(region);
		}
		ToolWindow previewWindow = previewWindowInOppositeRegion(region);
		if (previewWindow != null) {
			return previewWindow.component;
		}
		ToolWindow window = dockedWindowInRegion(region);
		return window == null ? null : window.component;
	}

	private ToolWindow previewWindowInOppositeRegion(int region) {
		if (!layoutPlaceholder.isVisible() || region == placeholderRegion) {
			return null;
		}
		ToolWindow window = singleDockedWindow();
		if (window == null) {
			return null;
		}
		return model.state(window.id).region() == placeholderRegion ? window : null;
	}

	private ToolWindow dockedWindowInRegion(int region) {
		for (ToolWindow window : windowsById.values()) {
			ToolWindowState state = model.state(window.id);
			if (state.mode() == ToolWindowMode.DOCKED && state.region() == region) {
				return window;
			}
		}
		return null;
	}

	private ToolWindow singleDockedWindow() {
		ToolWindow singleWindow = null;
		for (ToolWindow window : windowsById.values()) {
			if (model.state(window.id).mode() != ToolWindowMode.DOCKED) {
				continue;
			}
			if (singleWindow != null) {
				return null;
			}
			singleWindow = window;
		}
		return singleWindow;
	}

	private boolean hasDockedWindow() {
		for (ToolWindow window : windowsById.values()) {
			if (model.state(window.id).mode() == ToolWindowMode.DOCKED) {
				return true;
			}
		}
		return false;
	}

	private boolean shouldShowPanel(boolean hasDockedWindow, boolean hasPreview) {
		return isOverflowHost() || hasDockedWindow || hasPreview || hasWindowButton();
	}

	private boolean hasWindowButton() {
		for (ToolWindow window : windowsById.values()) {
			if (model.state(window.id).mode() != ToolWindowMode.HIDDEN) {
				return true;
			}
		}
		return false;
	}

	private boolean isOverflowHost() {
		return anchor == ToolWindowAnchor.LEFT;
	}

	private void makeRoomForDrop(String incomingId, int region) {
		ToolWindow window = singleDockedWindow();
		if (window != null && !window.id.equals(incomingId) && model.state(window.id).region() == region) {
			model.moveToRegion(window.id, oppositeRegion(region));
		}
	}

	private int oppositeRegion(int region) {
		return region == TOP_REGION ? BOTTOM_REGION : TOP_REGION;
	}

	private JComponent createLayoutPlaceholderContainer(int region) {
		int alignment = anchor == ToolWindowAnchor.BOTTOM && region == BOTTOM_REGION ? FlowLayout.RIGHT : FlowLayout.LEFT;
		JPanel row = new JPanel(new FlowLayout(alignment, 0, 0));
		row.setOpaque(false);
		row.add(layoutPlaceholder);
		JPanel container = new JPanel(new BorderLayout(0, 0));
		container.setOpaque(false);
		container.setBorder(new EmptyBorder(8, 8, 8, 8));
		if (anchor == ToolWindowAnchor.BOTTOM) {
			container.add(row, BorderLayout.NORTH);
		}
		else {
			container.add(row, region == TOP_REGION ? BorderLayout.NORTH : BorderLayout.SOUTH);
		}
		return container;
	}

	private JComponent createEmptyLayoutRegion() {
		JPanel panel = new JPanel(new BorderLayout());
		panel.setOpaque(false);
		return panel;
	}

	private JComponent createRegionalSplit(JComponent topComponent, JComponent bottomComponent) {
		JSplitPane splitPane = new JSplitPane(splitOrientation());
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(false);
		splitPane.setResizeWeight(restoredDivider());
		setSplitComponents(splitPane, topComponent, bottomComponent);
		splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, event -> saveDivider(splitPane));
		SwingUtilities.invokeLater(() -> splitPane.setDividerLocation(restoredDivider()));
		return splitPane;
	}

	private JComponent createPreviewSplit(JComponent topComponent, JComponent bottomComponent) {
		JSplitPane splitPane = new JSplitPane(splitOrientation());
		splitPane.setContinuousLayout(true);
		splitPane.setOneTouchExpandable(false);
		setSplitComponents(splitPane, topComponent, bottomComponent);
		SwingUtilities.invokeLater(() -> positionPreviewDivider(splitPane));
		return splitPane;
	}

	private int splitOrientation() {
		return anchor == ToolWindowAnchor.BOTTOM ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;
	}

	private void setSplitComponents(JSplitPane splitPane, JComponent topComponent, JComponent bottomComponent) {
		if (anchor == ToolWindowAnchor.BOTTOM) {
			splitPane.setLeftComponent(topComponent);
			splitPane.setRightComponent(bottomComponent);
		}
		else {
			splitPane.setTopComponent(topComponent);
			splitPane.setBottomComponent(bottomComponent);
		}
	}

	private void positionPreviewDivider(JSplitPane splitPane) {
		int availableSize = splitSize(splitPane);
		if (availableSize <= 0) {
			return;
		}
		int previewSize = anchor == ToolWindowAnchor.BOTTOM ? LAYOUT_PLACEHOLDER_SIZE.width + 24
				: LAYOUT_PLACEHOLDER_SIZE.height + 24;
		if (placeholderRegion == TOP_REGION) {
			splitPane.setDividerLocation(Math.min(previewSize, Math.max(1, availableSize - 1)));
		}
		else {
			splitPane.setDividerLocation(Math.max(1, availableSize - previewSize));
		}
	}

	private void saveState(ToolWindow window) {
		ToolWindowState state = model.state(window.id);
		ResourceController resourceController = ResourceController.getResourceController();
		resourceController.setProperty(stateKey(window.id, "anchor"), state.anchor().name());
		resourceController.setProperty(stateKey(window.id, "mode"), state.mode().name());
		resourceController.setProperty(stateKey(window.id, "region"), Integer.toString(state.region()));
		if ("/note".equals(window.id)) {
			resourceController.setProperty("note_location", state.anchor() == ToolWindowAnchor.LEFT ? "left" : "right");
		}
	}

	private void saveStates() {
		for (ToolWindow window : windowsById.values()) {
			saveState(window);
		}
	}

	private ToolWindowAnchor restoredAnchor(String id, ToolWindowAnchor fallback) {
		String value = ResourceController.getResourceController().getProperty(stateKey(id, "anchor"), fallback.name());
		try {
			ToolWindowAnchor restoredAnchor = ToolWindowAnchor.valueOf(value);
			return restoredAnchor == ToolWindowAnchor.BOTTOM ? fallback : restoredAnchor;
		}
		catch (IllegalArgumentException e) {
			return fallback;
		}
	}

	private ToolWindowMode restoredMode(String id, boolean visible) {
		String fallback = visible ? ToolWindowMode.DOCKED.name() : ToolWindowMode.HIDDEN.name();
		String value = ResourceController.getResourceController().getProperty(stateKey(id, "mode"), fallback);
		try {
			return ToolWindowMode.valueOf(value);
		}
		catch (IllegalArgumentException e) {
			return visible ? ToolWindowMode.DOCKED : ToolWindowMode.HIDDEN;
		}
	}

	private int restoredRegion(String id) {
		int region = ResourceController.getResourceController().getIntProperty(stateKey(id, "region"),
				ToolWindowLayoutModel.DEFAULT_REGION);
		return region == BOTTOM_REGION ? BOTTOM_REGION : TOP_REGION;
	}

	private double restoredDivider() {
		double ratio = ResourceController.getResourceController().getDoubleProperty(dividerKey(), 0.5d);
		return Math.max(0.12d, Math.min(0.88d, ratio));
	}

	private void saveDivider(JSplitPane splitPane) {
		if (!splitPane.isShowing()) {
			return;
		}
		int availableSize = splitSize(splitPane);
		if (availableSize <= 0) {
			return;
		}
		double location = (double) splitPane.getDividerLocation() / availableSize;
		ResourceController.getResourceController().setProperty(dividerKey(), Double.toString(location));
	}

	private int splitSize(JSplitPane splitPane) {
		int size = anchor == ToolWindowAnchor.BOTTOM ? splitPane.getWidth() : splitPane.getHeight();
		return size - splitPane.getDividerSize();
	}

	private String dividerKey() {
		return PROPERTY_PREFIX + anchor.name().toLowerCase() + ".divider";
	}

	private String sizeKey() {
		String sizeName = anchor == ToolWindowAnchor.BOTTOM ? "height" : "width";
		return PROPERTY_PREFIX + anchor.name().toLowerCase() + "." + sizeName;
	}

	private JPanel createResizeHandle() {
		CollapseHandle handle = new CollapseHandle(anchor);
		handle.setOpaque(false);
		handle.setPreferredSize(anchor == ToolWindowAnchor.BOTTOM ? new Dimension(1, RESIZE_HANDLE_SIZE)
				: new Dimension(RESIZE_HANDLE_SIZE, 1));
		handle.setCursor(Cursor.getPredefinedCursor(anchor == ToolWindowAnchor.BOTTOM ? Cursor.N_RESIZE_CURSOR
				: Cursor.E_RESIZE_CURSOR));
		MouseAdapter resizeHandler = new MouseAdapter() {
			private int startSize;
			private int startCoordinate;

			@Override
			public void mousePressed(MouseEvent event) {
				if (collapsed) {
					return;
				}
				startSize = anchor == ToolWindowAnchor.BOTTOM ? getHeight() : getWidth();
				Point location = event.getLocationOnScreen();
				startCoordinate = anchor == ToolWindowAnchor.BOTTOM ? location.y : location.x;
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				if (!collapsed) {
					savePreferredSize();
				}
			}

			@Override
			public void mouseDragged(MouseEvent event) {
				if (collapsed) {
					return;
				}
				Point location = event.getLocationOnScreen();
				int coordinate = anchor == ToolWindowAnchor.BOTTOM ? location.y : location.x;
				int delta = coordinate - startCoordinate;
				int size;
				if (anchor == ToolWindowAnchor.LEFT) {
					size = startSize + delta;
				}
				else {
					size = startSize - delta;
				}
				applyPreferredSize(size);
			}
		};
		handle.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				toggleCollapsed();
			}
		});
		handle.addMouseListener(resizeHandler);
		handle.addMouseMotionListener(resizeHandler);
		return handle;
	}

	private void toggleCollapsed() {
		setCollapsed(!collapsed);
	}

	private void setCollapsed(boolean collapsed) {
		if (this.collapsed == collapsed) {
			return;
		}
		this.collapsed = collapsed;
		content.setVisible(!collapsed);
		updatePreferredSize(hasDockedWindow(), layoutPlaceholder.isVisible());
		revalidate();
		repaint();
	}

	private int restoredPreferredSize() {
		int fallback = anchor == ToolWindowAnchor.BOTTOM ? DEFAULT_PANEL_HEIGHT : DEFAULT_PANEL_WIDTH;
		return ResourceController.getResourceController().getIntProperty(sizeKey(), fallback);
	}

	private void applyPreferredSize(int size) {
		preferredPanelSize = Math.max(MINIMUM_PANEL_SIZE, Math.min(maximumPanelSize(), size));
		updatePreferredSize(hasDockedWindow(), layoutPlaceholder.isVisible());
	}

	private void updatePreferredSize(boolean hasDockedWindow, boolean hasPreview) {
		if (collapsed && shouldShowPanel(hasDockedWindow, hasPreview)) {
			setPreferredSize(collapsedPreferredSize());
		}
		else if (hasDockedWindow) {
			setPreferredSize(preferredSize(preferredPanelSize));
		}
		else if (hasPreview) {
			setPreferredSize(preferredSize(previewPanelSize()));
		}
		else {
			setPreferredSize(null);
		}
		setVisible(shouldShowPanel(hasDockedWindow, hasPreview));
		Component parent = getParent();
		if (parent instanceof JComponent) {
			((JComponent) parent).revalidate();
			parent.repaint();
		}
		else {
			revalidate();
			repaint();
		}
	}

	private Dimension preferredSize(int size) {
		return anchor == ToolWindowAnchor.BOTTOM ? new Dimension(1, size) : new Dimension(size, 1);
	}

	private Dimension collapsedPreferredSize() {
		int size = STRIPE_BUTTON_SIZE.width + RESIZE_HANDLE_SIZE + 8;
		return anchor == ToolWindowAnchor.BOTTOM ? new Dimension(1, STRIPE_BUTTON_SIZE.height + RESIZE_HANDLE_SIZE + 8)
				: new Dimension(size, 1);
	}

	private int maximumPanelSize() {
		Component parent = getParent();
		int parentSize = anchor == ToolWindowAnchor.BOTTOM && parent != null ? parent.getHeight()
				: parent != null ? parent.getWidth() : 0;
		if (parentSize <= 0) {
			return 900;
		}
		return Math.max(MINIMUM_PANEL_SIZE, parentSize - 160);
	}

	private int previewPanelSize() {
		if (anchor == ToolWindowAnchor.BOTTOM) {
			return STRIPE_BUTTON_SIZE.height + RESIZE_HANDLE_SIZE + LAYOUT_PLACEHOLDER_SIZE.height + 32;
		}
		return STRIPE_BUTTON_SIZE.width + RESIZE_HANDLE_SIZE + LAYOUT_PLACEHOLDER_SIZE.width + 32;
	}

	private void savePreferredSize() {
		ResourceController.getResourceController().setProperty(sizeKey(), Integer.toString(preferredPanelSize));
	}

	private static String stateKey(String id, String property) {
		return PROPERTY_PREFIX + safeId(id) + "." + property;
	}

	private static String safeId(String id) {
		StringBuilder builder = new StringBuilder(id.length());
		for (int i = 0; i < id.length(); i++) {
			char c = id.charAt(i);
			builder.append(Character.isLetterOrDigit(c) ? c : '_');
		}
		return builder.toString();
	}

	private void showDropPlaceholder(boolean visible, int region, boolean showTabPlaceholder) {
		boolean tabVisible = visible && showTabPlaceholder;
		if (tabPlaceholder.isVisible() == tabVisible && layoutPlaceholder.isVisible() == visible
				&& placeholderRegion == region) {
			return;
		}
		placeholderRegion = region;
		tabPlaceholder.setVisible(tabVisible);
		layoutPlaceholder.setVisible(visible);
		rebuildTabs();
		rebuildDockedContent();
	}

	private ModernToolWindowPanel findDropTarget(Point screenPoint) {
		for (ModernToolWindowPanel panel : new ArrayList<ModernToolWindowPanel>(PANELS)) {
			if (panel.containsDropScreenPoint(screenPoint)) {
				return panel;
			}
		}
		return null;
	}

	private boolean containsDropScreenPoint(Point screenPoint) {
		return containsTabScreenPoint(screenPoint) || containsContentScreenPoint(screenPoint)
				|| containsLayoutPreviewScreenPoint(screenPoint);
	}

	private boolean containsTabScreenPoint(Point screenPoint) {
		if (anchor == ToolWindowAnchor.BOTTOM) {
			return false;
		}
		if (!stripe.isShowing()) {
			return false;
		}
		Rectangle bounds = new Rectangle(stripe.getLocationOnScreen(), stripe.getSize());
		return bounds.contains(screenPoint);
	}

	private boolean containsContentScreenPoint(Point screenPoint) {
		if (!content.isShowing()) {
			return false;
		}
		Rectangle bounds = new Rectangle(content.getLocationOnScreen(), content.getSize());
		return bounds.contains(screenPoint);
	}

	private boolean containsLayoutPreviewScreenPoint(Point screenPoint) {
		if (!layoutPlaceholder.isShowing()) {
			return false;
		}
		Rectangle bounds = new Rectangle(layoutPlaceholder.getLocationOnScreen(), layoutPlaceholder.getSize());
		return bounds.contains(screenPoint);
	}

	private int regionFor(Point screenPoint) {
		if (containsLayoutPreviewScreenPoint(screenPoint)) {
			return placeholderRegion;
		}
		if (containsContentScreenPoint(screenPoint)) {
			Point localPoint = new Point(screenPoint);
			SwingUtilities.convertPointFromScreen(localPoint, content);
			if (anchor == ToolWindowAnchor.BOTTOM) {
				return localPoint.x < content.getWidth() / 2 ? TOP_REGION : BOTTOM_REGION;
			}
			return localPoint.y < content.getHeight() / 2 ? TOP_REGION : BOTTOM_REGION;
		}
		return stripeRegionFor(screenPoint);
	}

	private int stripeRegionFor(Point screenPoint) {
		Point localPoint = new Point(screenPoint);
		SwingUtilities.convertPointFromScreen(localPoint, topTabs);
		int topRegionSize = tabRegionSize(TOP_REGION);
		if (topRegionSize <= 0) {
			return ToolWindowLayoutModel.DEFAULT_REGION;
		}
		if (anchor == ToolWindowAnchor.BOTTOM) {
			return localPoint.x <= topRegionSize ? TOP_REGION : BOTTOM_REGION;
		}
		return localPoint.y <= topRegionSize ? TOP_REGION : BOTTOM_REGION;
	}

	private int tabRegionSize(int region) {
		int count = tabPlaceholder.isVisible() && placeholderRegion == region ? 1 : 0;
		for (ToolWindow window : windowsById.values()) {
			ToolWindowState state = model.state(window.id);
			if (state.mode() != ToolWindowMode.HIDDEN && state.region() == region) {
				count++;
			}
		}
		return count * (anchor == ToolWindowAnchor.BOTTOM ? STRIPE_BUTTON_SIZE.width : STRIPE_BUTTON_SIZE.height);
	}

	private static void clearPlaceholders() {
		for (ModernToolWindowPanel panel : new ArrayList<ModernToolWindowPanel>(PANELS)) {
			panel.showDropPlaceholder(false, panel.placeholderRegion, false);
		}
	}

	private static void removeFromParent(Component component) {
		Component parent = component.getParent();
		if (parent instanceof JComponent) {
			((JComponent) parent).remove(component);
			parent.revalidate();
			parent.repaint();
		}
	}

	private static String toolbarId(JComponent component) {
		Object id = component.getClientProperty(ToolWindowClientProperties.TOOLBAR_ID);
		return id instanceof String ? (String) id : null;
	}

	private static String titleFor(String id) {
		if ("/icon_toolbar".equals(id)) {
			return TextUtils.getText("tool_window_icons", "Icons");
		}
		if ("/format".equals(id)) {
			return TextUtils.getText("tool_window_style", "Style");
		}
		if ("/filter_toolbar".equals(id)) {
			return TextUtils.getText("tool_window_filter", "Filter");
		}
		if ("/note".equals(id)) {
			return TextUtils.getText("tool_window_note", "Note");
		}
		if ("/outline".equals(id)) {
			return TextUtils.getText("tool_window_outline", "Outline");
		}
		if (id.startsWith("/")) {
			return id.substring(1).replace('_', ' ');
		}
		return id.replace('_', ' ');
	}

	private static Icon iconFor(String id) {
		if ("/format".equals(id)) {
			return new ToolWindowIcon(ToolWindowIcon.Kind.SLIDERS);
		}
		if ("/icon_toolbar".equals(id)) {
			return new ToolWindowIcon(ToolWindowIcon.Kind.GRID);
		}
		if ("/filter_toolbar".equals(id)) {
			return new ToolWindowIcon(ToolWindowIcon.Kind.FILTER);
		}
		if ("/note".equals(id)) {
			return new ToolWindowIcon(ToolWindowIcon.Kind.NOTE);
		}
		if ("/outline".equals(id)) {
			return new ToolWindowIcon(ToolWindowIcon.Kind.OUTLINE);
		}
		return new ToolWindowIcon(ToolWindowIcon.Kind.PANEL);
	}

	private class DragHandler extends MouseAdapter {
		private final String id;
		private Point pressScreenPoint;
		private boolean dragging;
		private boolean suppressNextAction;

		DragHandler(String id) {
			this.id = id;
		}

		@Override
		public void mousePressed(MouseEvent event) {
			pressScreenPoint = event.getLocationOnScreen();
			dragging = false;
			suppressNextAction = false;
			if (event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event)) {
				suppressButtonAction();
				showPopup(event);
			}
		}

		@Override
		public void mouseDragged(MouseEvent event) {
			ToolWindow window = windowsById.get(id);
			if (window == null) {
				return;
			}
			Point screenPoint = event.getLocationOnScreen();
			if (!dragging && pressScreenPoint != null && pressScreenPoint.distance(screenPoint) > 8) {
				dragging = true;
				suppressButtonAction();
			}
			if (dragging) {
				ModernToolWindowPanel dropTarget = findDropTarget(screenPoint);
				clearPlaceholders();
				if (dropTarget != null) {
					dropTarget.showDropPlaceholder(true, dropTarget.regionFor(screenPoint), true);
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent event) {
			if (event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event)) {
				suppressButtonAction();
				showPopup(event);
				pressScreenPoint = null;
				dragging = false;
				return;
			}
			if (dragging) {
				ToolWindow window = windowsById.get(id);
				Point screenPoint = event.getLocationOnScreen();
				ModernToolWindowPanel dropTarget = findDropTarget(screenPoint);
				clearPlaceholders();
				if (window != null) {
					if (dropTarget == null) {
						floatWindow(window, screenPoint);
					}
					else {
						int region = dropTarget.regionFor(screenPoint);
						if (dropTarget == ModernToolWindowPanel.this) {
							dockWindowInRegion(window, region);
						}
						else {
							transferTo(window, dropTarget, region);
						}
					}
				}
			}
			pressScreenPoint = null;
			dragging = false;
		}

		private boolean consumeActionSuppression() {
			boolean suppressed = suppressNextAction;
			suppressNextAction = false;
			return suppressed;
		}

		private void suppressButtonAction() {
			suppressNextAction = true;
		}

		private void showPopup(MouseEvent event) {
			ToolWindow window = windowsById.get(id);
			if (window != null) {
				createPopup(window).show(event.getComponent(), event.getX(), event.getY());
			}
		}
	}

	private static class DashedBorder extends AbstractBorder {
		private static final long serialVersionUID = 1L;

		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				float[] dash = new float[] {4f, 4f};
				g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, dash, 0));
				g2.setColor(UIManager.getColor("Component.borderColor"));
				g2.drawRoundRect(x + 4, y + 4, width - 9, height - 9, 8, 8);
			}
			finally {
				g2.dispose();
			}
		}
	}

	private static class TabSeparator extends JPanel {
		private static final long serialVersionUID = 1L;
		private final ToolWindowAnchor anchor;

		TabSeparator(ToolWindowAnchor anchor) {
			this.anchor = anchor;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				Color color = UIManager.getColor("Component.borderColor");
				if (color == null) {
					color = UIManager.getColor("Separator.foreground");
				}
				if (color == null) {
					color = new Color(0x5F6368);
				}
				g2.setColor(color);
				if (anchor == ToolWindowAnchor.BOTTOM) {
					int x = getWidth() / 2;
					g2.drawLine(x, 6, x, Math.max(6, getHeight() - 6));
				}
				else {
					int y = getHeight() / 2;
					g2.drawLine(6, y, Math.max(6, getWidth() - 6), y);
				}
			}
			finally {
				g2.dispose();
			}
		}
	}

	private static class CollapseHandle extends JPanel {
		private static final long serialVersionUID = 1L;
		private final ToolWindowAnchor anchor;
		private boolean hovered;

		CollapseHandle(ToolWindowAnchor anchor) {
			this.anchor = anchor;
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent event) {
					hovered = true;
					repaint();
				}

				@Override
				public void mouseExited(MouseEvent event) {
					hovered = false;
					repaint();
				}
			});
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (!hovered) {
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				Color color = UIManager.getColor("Component.accentColor");
				if (color == null) {
					color = UIManager.getColor("Actions.Blue");
				}
				if (color == null) {
					color = new Color(0x3574F0);
				}
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(color);
				int centerX = getWidth() / 2;
				int centerY = getHeight() / 2;
				if (anchor == ToolWindowAnchor.LEFT) {
					g2.drawLine(centerX - 1, centerY - 5, centerX + 2, centerY);
					g2.drawLine(centerX + 2, centerY, centerX - 1, centerY + 5);
				}
				else if (anchor == ToolWindowAnchor.RIGHT) {
					g2.drawLine(centerX + 1, centerY - 5, centerX - 2, centerY);
					g2.drawLine(centerX - 2, centerY, centerX + 1, centerY + 5);
				}
				else {
					g2.drawLine(centerX - 5, centerY + 1, centerX, centerY - 2);
					g2.drawLine(centerX, centerY - 2, centerX + 5, centerY + 1);
				}
			}
			finally {
				g2.dispose();
			}
		}
	}

	private static class LayoutPreviewPanel extends JPanel {
		private static final long serialVersionUID = 1L;

		LayoutPreviewPanel() {
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				Color accent = UIManager.getColor("Component.accentColor");
				if (accent == null) {
					accent = UIManager.getColor("Actions.Blue");
				}
				if (accent == null) {
					accent = new Color(0x3574F0);
				}
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 52));
				g2.fillRoundRect(4, 4, Math.max(0, getWidth() - 8), Math.max(0, getHeight() - 8), 8, 8);
			}
			finally {
				g2.dispose();
			}
			super.paintComponent(g);
		}
	}

	private static class ToolWindowIcon implements Icon {
		enum Kind {
				SLIDERS,
				GRID,
				FILTER,
				NOTE,
				OUTLINE,
				PANEL,
				MORE
		}

		private final Kind kind;

		ToolWindowIcon(Kind kind) {
			this.kind = kind;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(c.getForeground() != null ? c.getForeground() : UIManager.getColor("Button.foreground"));
				g2.setStroke(stroke());
				int left = x + 5;
				int top = y + 5;
				switch (kind) {
					case SLIDERS:
						line(g2, left, top + 2, left + 14, top + 2);
						line(g2, left, top + 8, left + 14, top + 8);
						line(g2, left, top + 14, left + 14, top + 14);
						circle(g2, left + 4, top + 2);
						circle(g2, left + 10, top + 8);
						circle(g2, left + 7, top + 14);
						break;
					case GRID:
						for (int row = 0; row < 2; row++) {
							for (int column = 0; column < 2; column++) {
								g2.drawRoundRect(left + column * 8, top + row * 8, 5, 5, 2, 2);
							}
						}
						break;
					case FILTER:
						g2.drawLine(left, top, left + 14, top);
						g2.drawLine(left + 2, top, left + 7, top + 7);
						g2.drawLine(left + 12, top, left + 7, top + 7);
						g2.drawLine(left + 7, top + 7, left + 7, top + 14);
						g2.drawLine(left + 7, top + 14, left + 11, top + 12);
						break;
					case NOTE:
						g2.drawRoundRect(left + 2, top, 11, 15, 2, 2);
						line(g2, left + 5, top + 4, left + 11, top + 4);
						line(g2, left + 5, top + 8, left + 11, top + 8);
						line(g2, left + 5, top + 12, left + 9, top + 12);
						break;
						case OUTLINE:
							circle(g2, left + 2, top + 2);
							circle(g2, left + 2, top + 8);
							circle(g2, left + 2, top + 14);
							line(g2, left + 6, top + 2, left + 14, top + 2);
							line(g2, left + 6, top + 8, left + 14, top + 8);
							line(g2, left + 6, top + 14, left + 14, top + 14);
							break;
						case PANEL:
							g2.drawRoundRect(left, top, 15, 15, 3, 3);
							line(g2, left + 4, top, left + 4, top + 15);
							break;
						case MORE:
							circle(g2, left + 3, top + 8);
							circle(g2, left + 8, top + 8);
							circle(g2, left + 13, top + 8);
							break;
					}
			}
			finally {
				g2.dispose();
			}
		}

		@Override
		public int getIconWidth() {
			return 24;
		}

		@Override
		public int getIconHeight() {
			return 24;
		}

		private Stroke stroke() {
			return new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		}

		private void line(Graphics2D g2, int x1, int y1, int x2, int y2) {
			g2.drawLine(x1, y1, x2, y2);
		}

		private void circle(Graphics2D g2, int centerX, int centerY) {
			g2.drawOval(centerX - 1, centerY - 1, 2, 2);
		}
	}

	private static class ToolWindow {
		final String id;
		final String title;
		final JComponent component;
		final JToggleButton button;
		final int index;
		JFrame floatingFrame;

		ToolWindow(String id, String title, JComponent component, JToggleButton button, int index) {
			this.id = id;
			this.title = title;
			this.component = component;
			this.button = button;
			this.index = index;
		}
	}
}
