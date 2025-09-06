package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.Window;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.FocusManager;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.KeyStroke;
import javax.swing.Icon;

class ScrollableTreePanel extends JPanel {
	private static final long serialVersionUID = 1;
    private static final int BLOCK_SIZE = 50;

    private static final MouseListener focusSelectedButtonOnClick = new MouseAdapter() {
		@Override
		public void mouseClicked(MouseEvent e) {
			((ScrollableTreePanel)e.getComponent()).focusSelectionButtonLater(true);
		}
	};

	private  final NavigationButtons navButtons;
    final SelectionCircleIcon selectionIcon;
    private final BreadcrumbPanel breadcrumbPanel;
    final OutlineGeometry geometry;
    private final ExpansionControls expansionControls;
    private NodePositioning nodePositioning;
    private  final BreadcrumbPath breadcrumbPath;
    private  OutlineViewport viewport;

    private TreeNode root;
    private OutlineSelection outlineSelection;
    private final int blockSize;
    private VisibleOutlineState visibleState;
    private final OutlineBlockViewCache blockCache = new OutlineBlockViewCache();
    private OutlineSelectionBridge selectionBridge;
    private final Map<String, String> lastSelectedChildByParent = new HashMap<>();


    private int lastFirstBlock = -1;
    private int lastLastBlock = -1;
    private int lastBreadcrumbAreaHeight = -1;
    private int lastViewportWidth = -1;
    private int lastVisibleNodeCount = -1;

    ScrollableTreePanel(TreeNode root,  BreadcrumbPanel breadcrumbPanel) {
		this(root, BLOCK_SIZE,breadcrumbPanel);
		addMouseListener(focusSelectedButtonOnClick);
	}

    private ScrollableTreePanel(TreeNode root, int blockSize, BreadcrumbPanel breadcrumbPanel) {
        super(null);
        this.root = root;
        this.blockSize = blockSize;
        this.breadcrumbPanel = breadcrumbPanel;
        root.applyExpansionLevel(1);
        this.outlineSelection = new OutlineSelection(root);
        this.visibleState = new VisibleOutlineState(root);

        this.geometry = new OutlineGeometry(new JButton("▶"));
        this.expansionControls = new ExpansionControls(this);
        this.nodePositioning = new NodePositioning(root, geometry, visibleState);
        this.breadcrumbPath = new BreadcrumbPath(root, geometry, visibleState, null);
        this.navButtons = new NavigationButtons(geometry, expansionControls);
        this.selectionIcon = new SelectionCircleIcon(Color.BLUE, geometry.iconDiameter);


        setFocusable(true);
        setupKeyBindings();

        add(navButtons.expandBtn);
        add(navButtons.collapseBtn);
        add(navButtons.expandMoreBtn);
        add(navButtons.reduceBtn);

        navButtons.hideNavigationButtons();
    }

    @SuppressWarnings("serial")
	private void setupKeyBindings() {
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "navigateUp");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "navigateDown");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "navigatePageUp");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "navigatePageDown");
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "goParent");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "goChild");
        inputMap.put(KeyStroke.getKeyStroke("control LEFT"), "reduceExpansion");
        inputMap.put(KeyStroke.getKeyStroke("control RIGHT"), "expandMore");

        actionMap.put("navigateUp", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { navigateUp(); }
        });
        actionMap.put("navigateDown", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { navigateDown(); }
        });
        actionMap.put("navigatePageUp", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { navigatePageUp(); }
        });
        actionMap.put("navigatePageDown", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { navigatePageDown(); }
        });
        actionMap.put("goParent", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { goToParent(); }
        });
        actionMap.put("goChild", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { goToChild(); }
        });
        actionMap.put("reduceExpansion", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { reduceSelectedExpansion(); }
        });
        actionMap.put("expandMore", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { expandSelectedMore(); }
        });
    }

    private boolean focusButtonInBreadcrumbForNode(TreeNode node) {
        if (node == null) return false;
        for (Component comp : breadcrumbPanel.getComponents()) {
            if (comp instanceof NodeButton) {
                NodeButton btn = (NodeButton) comp;
                if (btn.getNode() == node && btn.isShowing()) {
                    btn.requestFocusInWindow();
                    return true;
                }
            }
        }
        return false;
    }

    private boolean focusButtonInBlocksForNode(TreeNode node) {
        if (node == null) return false;
        for (BlockPanel panel : blockCache.values()) {
            for (Component comp : panel.getComponents()) {
                if (comp instanceof NodeButton) {
                    NodeButton btn = (NodeButton) comp;
                    if (btn.getNode() == node && btn.isShowing()) {
                        btn.requestFocusInWindow();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    void synchronizeSelectionButton(boolean requestFocusInWindow) {
    	selectionBridge.synchronizeOutlineSelection(requestFocusInWindow);
    	focusSelectionButton(requestFocusInWindow);
    }

    private void focusSelectionButton(boolean requestFocusInWindow) {
        TreeNode selected = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (selected == null) {
            return;
        }
        scrollToSelectedNode();
        if(! requestFocusInWindow) {
        	final Component focusOwner = FocusManager.getCurrentManager().getCurrentFocusCycleRoot();
        	if (! SwingUtilities.isDescendingFrom(focusOwner, this)) {
        		selectionBridge.focusMapNode();
				return;
			}
        }
        TreeNode n = outlineSelection.getSelectedNode();
        while (n != null) {
            if (focusButtonInBreadcrumbForNode(n)) return;
            if (focusButtonInBlocksForNode(n)) return;
            n = n.getParent();
        }
    }



    void setScrollPane(JScrollPane scroll) {
        this.viewport = new OutlineViewport(scroll, geometry, visibleState, nodePositioning);
        this.breadcrumbPath.setViewport(viewport);
        resetBlockCache();
    }

    void setBreadcrumbAreaHeight(int height) {
        this.visibleState.setBreadcrumbAreaHeight(height);
    }



        void updateVisibleBlocks() {
        if (viewport == null) return;
        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = isWithinOutline(prevFocus);


        OutlineViewport.VisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        int viewportWidth = viewport.getViewportWidth();
        int visibleCount = visibleState.getVisibleNodeCount();
        boolean haveBlocks = !blockCache.isEmpty();
        boolean unchanged = haveBlocks
                && range.getFirstBlock() == lastFirstBlock
                && range.getLastBlock() == lastLastBlock
                && range.getBreadcrumbAreaHeight() == lastBreadcrumbAreaHeight
                && viewportWidth == lastViewportWidth
                && visibleCount == lastVisibleNodeCount;

        if (unchanged) {
            viewport.refreshViewport();
            // keep first visible node id in sync
            updateFirstVisibleNodeId();
            return;
        }

        clearBlocks();
        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();


        lastFirstBlock = range.getFirstBlock();
        lastLastBlock = range.getLastBlock();
        lastBreadcrumbAreaHeight = range.getBreadcrumbAreaHeight();
        lastViewportWidth = viewportWidth;
        lastVisibleNodeCount = visibleCount;
        updateFirstVisibleNodeId();

        TreeNode hoveredNode = visibleState.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.getChildren().isEmpty()) {

            boolean isInBreadcrumb = visibleState.isNodeInBreadcrumbArea(hoveredNode, geometry.rowHeight);

            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, visibleState.getBreadcrumbAreaHeight(), nodePositioning);
            }
        }
        restoreFocusIfNeeded(prevInOutline);
    }

    void updateVisibleBlocks(int startFromNodeIndex) {
        if (viewport == null) return;
        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = isWithinOutline(prevFocus);

        clearBlocks();


        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        viewport.setViewPosition(startFromNodeIndex, breadcrumbAreaHeight);

        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();


        OutlineViewport.VisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        lastFirstBlock = range.getFirstBlock();
        lastLastBlock = range.getLastBlock();
        lastBreadcrumbAreaHeight = range.getBreadcrumbAreaHeight();
        lastViewportWidth = viewport.getViewportWidth();
        lastVisibleNodeCount = visibleState.getVisibleNodeCount();
        updateFirstVisibleNodeId();

        TreeNode hoveredNode = visibleState.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.getChildren().isEmpty()) {

            boolean isInBreadcrumb = visibleState.isNodeInBreadcrumbArea(hoveredNode, geometry.rowHeight);

            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, breadcrumbAreaHeight, nodePositioning);
            }
        }
        restoreFocusIfNeeded(prevInOutline);
    }

    private void clearBlocks() {
        for (BlockPanel panel : blockCache.values()) {
            remove(panel);
        }
        blockCache.clear();

        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof BlockPanel) {
                remove(comp);
            }
        }

        components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JButton && !isNavigationButton((JButton) comp)) {
                remove(comp);
            }
        }
        revalidate();
    }

    private void resetBlockCache() {
        lastFirstBlock = -1;
        lastLastBlock = -1;
        lastBreadcrumbAreaHeight = -1;
        lastViewportWidth = -1;
        lastVisibleNodeCount = -1;
    }

    boolean isNodeVisibleInBlocks(TreeNode node) {
        for (BlockPanel panel : blockCache.values()) {
            for (Component comp : panel.getComponents()) {
                if (comp instanceof NodeButton) {
                    NodeButton btn = (NodeButton) comp;
                    if (btn.getNode() == node && comp.isShowing()) return true;
                }
            }
        }
        return false;
    }

    private boolean isNavigationButton(JButton button) {
        return button == navButtons.expandBtn || button == navButtons.collapseBtn ||
               button == navButtons.expandMoreBtn || button == navButtons.reduceBtn;
    }

    private void createVisibleBlocks() {
        OutlineViewport.VisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);

        for (int b = range.getFirstBlock(); b <= range.getLastBlock(); b++) {
            if (!blockCache.has(b))
                createBlock(b, range.getBreadcrumbAreaHeight());
        }
    }

    private void createBlock(int blockIndex, int yOffset) {
        int start = blockIndex * blockSize;
        int end = Math.min(start + blockSize, visibleState.getVisibleNodeCount());
        int breadcrumbNodeCount = visibleState.getBreadcrumbAreaHeight() / geometry.rowHeight;


        if (end <= breadcrumbNodeCount) {
            return;
        }

        java.util.List<FlatNode> blockNodes = new java.util.ArrayList<>();
        for (int i = start; i < end; i++) {
            FlatNode fn = visibleState.getFlatNodeAtIndex(i);
            if (fn != null) blockNodes.add(fn);
        }
        BlockPanel bp = new BlockPanel(blockNodes, start, geometry.rowHeight, this, breadcrumbNodeCount, outlineSelection);

        Rectangle bounds = nodePositioning.calculateBlockBounds(blockIndex, blockSize, yOffset, getPreferredSize().width);
        bp.setBounds(bounds);
        add(bp);
        blockCache.put(blockIndex, bp);
    }

    private void updatePreferredFromActualBlocks() {
        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
        int contentNodesCount = Math.max(0, visibleState.getVisibleNodeCount() - breadcrumbNodeCount);
        int height = breadcrumbAreaHeight + (contentNodesCount + 1) * geometry.rowHeight;
        int maxWidth = calculateActualRequiredWidth();
        setPreferredSize(new Dimension(maxWidth, height));

        for (BlockPanel panel : blockCache.values()) {
            Dimension currentSize = panel.getSize();
            panel.setSize(maxWidth, currentSize.height);
        }
    }

    private int calculateActualRequiredWidth() {
        int maxWidth = 400;
        for (BlockPanel panel : blockCache.values()) {
            Component[] components = panel.getComponents();
            for (Component comp : components) {
                if (comp instanceof JButton) {
                    int rightEdge = comp.getX() + comp.getWidth();
                    maxWidth = Math.max(maxWidth, rightEdge);
                }
            }
        }
        return maxWidth + 20;
    }

    private void refreshUI() {
        viewport.refreshViewport();
        repaint();
    }

    private void updateFirstVisibleNodeId() {
        if (viewport == null) return;
        int index = viewport.calculateFirstVisibleNodeIndex();
        int count = visibleState.getVisibleNodeCount();
        if (count == 0) {
            visibleState.setFirstVisibleNodeId(null);
            return;
        }
        index = Math.max(0, Math.min(index, count - 1));
        visibleState.setFirstVisibleNodeId(visibleState.getNodeIdAtVisibleIndex(index));
    }

    void setSelectedNode(TreeNode node, boolean requestFocus) {
		focusSelectionButtonLater(requestFocus);
		if (node.getParent() != null) {
			lastSelectedChildByParent.put(node.getParent().getId(), node.getId());
		}
		outlineSelection.selectNode(node);
		repaint();
		if(visibleState.findNodeIndexInVisibleList(node) < 0) {
			TreeNode preservedHoveredNode = visibleState.getHoveredNode();
			removeAll();
			blockCache.clear();
			navButtons.hideNavigationButtons();
			visibleState.setHoveredNode(preservedHoveredNode);
			updateVisibleBlocks();
		}
    }


    OutlineSelection getOutlineSelection() {
        return outlineSelection;
    }

    int getRowHeight() {
        return geometry.rowHeight;
    }

    int calcTextButtonX(int depth) {
        return geometry.calculateTextButtonX(depth);
    }

    int getViewportWidth() {
        return viewport != null ? viewport.getViewportWidth() : getWidth();
    }

    VisibleOutlineState getVisibleState() {
        return visibleState;
    }

    NodePositioning getNodePositioning() {
        return nodePositioning;
    }

    TreeNode getRoot() {
        return root;
    }



    private void scrollToSelectedNode() {
        TreeNode selectedNode = outlineSelection.getSelectedNode();
        if (selectedNode == null || viewport == null) return;

        // Try to find the actual button for the selected node in visible block panels
        Rectangle target = null;
        for (BlockPanel panel : blockCache.values()) {
            for (Component comp : panel.getComponents()) {
                if (comp instanceof NodeButton) {
                    NodeButton btn = (NodeButton) comp;
                    if (btn.getNode() == selectedNode) {
                        Rectangle b = comp.getBounds();
                        Rectangle p = panel.getBounds();
                        target = new Rectangle(b.x + p.x, b.y + p.y,
                                b.width + geometry.iconDiameter + 6,
                                Math.max(geometry.rowHeight * 2, geometry.iconDiameter + 4));
                        break;
                    }
                }
            }
            if (target != null) break;
        }
        if (target == null) {
            int nodeIndex = visibleState.findNodeIndexInVisibleList(selectedNode);
            if (nodeIndex >= 0) {
                int y = visibleState.getBreadcrumbAreaHeight() + nodeIndex * geometry.rowHeight;
                target = new Rectangle(0, y, viewport.getViewportWidth(), geometry.rowHeight * 2);
            }
        }
        if (target != null) {
            scrollRectToVisible(target);
        }
    }



    void selectMapNodeById(String nodeId) {
    	if (selectionBridge != null)
    		selectionBridge.selectMapNodeById(nodeId);
    }

    void setSelectionBridge(OutlineSelectionBridge bridge) {
        this.selectionBridge = bridge;
        breadcrumbPanel.setSelectionBridge(bridge);
    }

    void updateNodeTitle(TreeNode node) {
        for (Component comp : breadcrumbPanel.getComponents()) {
            if (comp instanceof NodeButton) {
                NodeButton btn = (NodeButton) comp;
                if (btn.getNode() == node) {
                    btn.setText(node.getTitle());
                    int depth = calculateNodeDepth(node);
                    if (depth >= 0) {
                        int x = geometry.calculateTextButtonX(depth);
                        btn.setBounds(x, btn.getY(), btn.getPreferredSize().width, geometry.rowHeight);
                    }
                    breadcrumbPanel.revalidate();
                    breadcrumbPanel.repaint();
                    break;
                }
            }
        }

        for (int blockIndex : blockCache.keySet()) {
            BlockPanel panel = blockCache.get(blockIndex);
            if (panel == null) continue;
            for (Component comp : panel.getComponents()) {
                if (comp instanceof NodeButton) {
                    NodeButton btn = (NodeButton) comp;
                    if (btn.getNode() == node) {
                        btn.setText(node.getTitle());
                        int depth = calculateNodeDepth(node);
                        if (depth >= 0) {
                            int x = geometry.calculateTextButtonX(depth);
                            btn.setBounds(x, btn.getY(), btn.getPreferredSize().width, geometry.rowHeight);
                        }
                        panel.revalidate();
                        panel.repaint();

                        break;
                    }
                }
            }
        }

        updatePreferredFromActualBlocks();
        refreshUI();
    }


    void rebuildFromNode(TreeNode anchorNode) {
        if (viewport == null) return;


        navButtons.hideNavigationButtons();

        visibleState.updateVisibleNodes();


        BreadcrumbState state = calculateBreadcrumbState();
        if (state != null) {
            breadcrumbPanel.update(state);
            updateVisibleBlocks(state.getFirstVisibleNodeIndex());

            TreeNode hovered = visibleState.getHoveredNode();
            if (hovered != null && visibleState.findNodeIndexInVisibleList(hovered) < 0) {
                visibleState.setHoveredNode(null);
            }
            return;
        }

        int anchorIndex = visibleState.findNodeIndexInVisibleList(anchorNode);
        if (anchorIndex < 0) {

            updateVisibleBlocksAndBreadcrumb();
            return;
        }

        OutlineViewport.VisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        int startBlock = Math.max(0, anchorIndex / blockSize);

        removeBlocksFromBlockIndex(startBlock);
        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();


        lastFirstBlock = range.getFirstBlock();
        lastLastBlock = range.getLastBlock();
        lastBreadcrumbAreaHeight = range.getBreadcrumbAreaHeight();
        lastViewportWidth = viewport.getViewportWidth();
        lastVisibleNodeCount = visibleState.getVisibleNodeCount();


        TreeNode hovered = visibleState.getHoveredNode();
        if (hovered != null && visibleState.findNodeIndexInVisibleList(hovered) < 0) {
            visibleState.setHoveredNode(null);
        }
    }

    private void removeBlocksFromBlockIndex(int startBlock) {
        List<Integer> indices = new ArrayList<>(blockCache.keySet());
        for (int idx : indices) {
            if (idx >= startBlock) {
                BlockPanel p = blockCache.get(idx);
                if (p != null) remove(p);
                blockCache.remove(idx);
            }
        }
    }

    void onContentButtonHovered(TreeNode node) {
        TreeNode hoveredNode = visibleState.getHoveredNode();
        if (node != null && !node.getChildren().isEmpty() && node != hoveredNode) {

            if (visibleState.isNodeInBreadcrumbArea(node, geometry.rowHeight)) {

                return;
            }

            visibleState.setHoveredNode(node);
            navButtons.attachToNode(node, this, false, -1, visibleState.getBreadcrumbAreaHeight(), nodePositioning);
            repaint();
        }
    }

        void refreshWithBreadcrumbs() {
        TreeNode preservedHoveredNode = visibleState.getHoveredNode();
        SwingUtilities.invokeLater(() -> {

            navButtons.hideNavigationButtons();


            updateVisibleBlocksAndBreadcrumb();


            if (preservedHoveredNode != null && !preservedHoveredNode.getChildren().isEmpty()) {
                visibleState.setHoveredNode(preservedHoveredNode);
                boolean isInBreadcrumb = visibleState.isNodeInBreadcrumbArea(preservedHoveredNode, geometry.rowHeight);

                if (isInBreadcrumb) {
                    breadcrumbPanel.updateNavigationButtons();
                } else {
                    navButtons.attachToNode(preservedHoveredNode, this, false, -1, visibleState.getBreadcrumbAreaHeight(), nodePositioning);
                }
            }
        });
    }

    void navigateUp() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            if (currentIndex > 0) {
                FlatNode prev = visibleState.getFlatNodeAtIndex(currentIndex - 1);
                if (prev != null) setSelectedNode(prev.node, true);
            }
        }
    }

	private void focusSelectionButtonLater(boolean requestFocus) {
		SwingUtilities.invokeLater(() -> focusSelectionButton(requestFocus));
	}

    void navigateDown() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int size = visibleState.getVisibleNodeCount();
            if (currentIndex >= 0 && currentIndex < size - 1) {
                FlatNode next = visibleState.getFlatNodeAtIndex(currentIndex + 1);
                if (next != null) setSelectedNode(next.node, true);
            }
        }
    }

    void navigatePageUp() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getPageSize();
            int newIndex = Math.max(0, currentIndex - pageSize);
            if (newIndex != currentIndex) {
                FlatNode fn = visibleState.getFlatNodeAtIndex(newIndex);
                if (fn != null) setSelectedNode(fn.node, true);
            }
        }
    }

    void navigatePageDown() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getPageSize();
            int size = visibleState.getVisibleNodeCount();
            int newIndex = Math.min(size - 1, currentIndex + pageSize);
            if (newIndex != currentIndex) {
                FlatNode fn = visibleState.getFlatNodeAtIndex(newIndex);
                if (fn != null) setSelectedNode(fn.node, true);
            }
        }
    }

    void toggleExpandSelected() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null) return;
        if (node.isExpanded()) {
            expansionControls.collapseNode(node);
        } else {
            expansionControls.expandNode(node);
        }
    }

    void expandSelectedMore() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null) return;
        expansionControls.expandNodeMore(node);
    }

    void reduceSelectedExpansion() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null) return;
        expansionControls.reduceNodeExpansion(node);
    }

    void goToParent() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null) return;
        final TreeNode newSelected = node.getParent();
		if (newSelected != null) {

        	setSelectedNode(newSelected, true);
        }
    }

    void goToChild() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null || node.getChildren().isEmpty()) return;
        if (!node.isExpanded()) {
            expansionControls.expandNode(node);
        }
        String preferredChildId = lastSelectedChildByParent.get(node.getId());
        TreeNode targetChild = null;
        if (preferredChildId != null) {
            for (TreeNode c : node.getChildren()) {
                if (preferredChildId.equals(c.getId())) { targetChild = c; break; }
            }
        }
        if (targetChild == null) targetChild = node.getChildren().get(0);

        setSelectedNode(targetChild, true);
    }

    private int getPageSize() {
        if (viewport != null) {
            return viewport.getPageSize();
        }
        return 10;
    }



    private List<TreeNode> getCurrentBreadcrumbNodes() {
    	return breadcrumbPanel.getCurrentBreadcrumbNodes();
    }

    void updateVisibleNodes() {
        visibleState.updateVisibleNodes();
        updateVisibleBlocksAndBreadcrumb();
    }

	void updateVisibleBlocksAndBreadcrumb() {
		Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		boolean prevInOutline = isWithinOutline(prevFocus);
		BreadcrumbState state = calculateBreadcrumbState();
	    if (state != null) {
	        breadcrumbPanel.update(state);
	        updateVisibleBlocks(state.getFirstVisibleNodeIndex());
	    }
	    else {

	        updateVisibleBlocks();
	    }
	    restoreFocusIfNeeded(prevInOutline);
	}

    private BreadcrumbState calculateBreadcrumbState() {
        List<TreeNode> currentBreadcrumbNodes = getCurrentBreadcrumbNodes();
        return breadcrumbPath.calculateBreadcrumbState(currentBreadcrumbNodes);
    }



    int calculateNodeDepth(TreeNode node) {
        return nodePositioning.calculateNodeDepth(node);
    }

    @Override
protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

	void attachNavigationNode(TreeNode node,
	        boolean isBreadCrumb, int rowIndex, int currentBreadcrumbHeight) {
		visibleState.setHoveredNode(node);
		navButtons.attachToNode(node, breadcrumbPanel, isBreadCrumb, rowIndex, currentBreadcrumbHeight, nodePositioning);
	}

    boolean isNodeInBreadcrumbArea(TreeNode node) {
        return visibleState.isNodeInBreadcrumbArea(node, geometry.rowHeight);
    }

    boolean isNodeInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return breadcrumbPath.isNodeInBreadcrumbPath(node, breadcrumbNodes);
    }

    int findNodeIndexInBreadcrumbPath(TreeNode node, List<TreeNode> breadcrumbNodes) {
        return breadcrumbPath.findNodeIndexInBreadcrumbPath(node, breadcrumbNodes);
    }

    Icon getSelectionIcon() {
        return selectionIcon;
    }

    boolean areNavButtonsVisible() {
        return navButtons.expandBtn.isVisible() || navButtons.collapseBtn.isVisible()
                || navButtons.expandMoreBtn.isVisible() || navButtons.reduceBtn.isVisible();
    }



    private boolean isWithinOutline(Component c) {
        if (c == null) return false;
        return SwingUtilities.isDescendingFrom(c, this) || SwingUtilities.isDescendingFrom(c, breadcrumbPanel);
    }

    private void restoreFocusIfNeeded(boolean previousWasInOutline) {
        if (!previousWasInOutline) return;

        Window w = SwingUtilities.getWindowAncestor(this);
        if (w == null || !w.isDisplayable()) return;

        Component current = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (isWithinOutline(current)) return;

        TreeNode selected = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (selected != null) {
            for (Component comp : breadcrumbPanel.getComponents()) {
                if (comp instanceof NodeButton) {
                    NodeButton btn = (NodeButton) comp;
                    if (btn.getNode() == selected && btn.isShowing()) {
                        btn.requestFocusInWindow();
                        return;
                    }
                }
            }
            for (BlockPanel panel : blockCache.values()) {
                for (Component comp : panel.getComponents()) {
                    if (comp instanceof NodeButton) {
                        NodeButton btn = (NodeButton) comp;
                        if (btn.getNode() == selected && btn.isShowing()) {
                            btn.requestFocusInWindow();
                            return;
                        }
                    }
                }
            }
        }
        if (isShowing()) requestFocusInWindow();
    }
}
