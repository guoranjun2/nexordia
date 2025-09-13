package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.List;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

class ScrollableTreePanel extends JPanel implements OutlineActionTarget {
	private static final long serialVersionUID = 1;
    private static final int BLOCK_SIZE = 50;



	private  final NavigationButtons navButtons;
	private final SelectionCircleIcon selectionIcon;
    private final BreadcrumbPanel breadcrumbPanel;
    final OutlineGeometry geometry;
    private final ExpansionControls expansionControls;
    private NodePositioning nodePositioning;
    private  final BreadcrumbPath breadcrumbPath;
    private  OutlineViewport viewport;

    private TreeNode root;
    private final OutlineSelection outlineSelection;
    private final int blockSize;
    private VisibleOutlineState visibleState;
    private final OutlineBlockViewCache blockCache = new OutlineBlockViewCache();
    private OutlineBlockLayout blockLayout;
    private OutlineSelectionBridge selectionBridge;
    private OutlineFocusManager focusManager;
    private final OutlineSelectionManager selectionManager = new OutlineSelectionManager();


    private int lastFirstBlock = -1;
    private int lastLastBlock = -1;
    private int lastBreadcrumbAreaHeight = -1;
    private int lastViewportWidth = -1;
    private int lastVisibleNodeCount = -1;

    ScrollableTreePanel(TreeNode root,  BreadcrumbPanel breadcrumbPanel) {
        this(root, BLOCK_SIZE,breadcrumbPanel);
        addMouseListener(new FocusSelectedButtonClickAdapter(focusManager));
	}

    private ScrollableTreePanel(TreeNode root, int blockSize, BreadcrumbPanel breadcrumbPanel) {
        super(null);
        this.root = root;
        this.blockSize = blockSize;
        this.breadcrumbPanel = breadcrumbPanel;
        this.outlineSelection = new OutlineSelection(root);
        this.visibleState = new VisibleOutlineState(root);

        this.geometry = new OutlineGeometry(new JButton("▶"));
        this.expansionControls = new ExpansionControls(this, outlineSelection);
        this.nodePositioning = new NodePositioning(geometry, visibleState);
        this.breadcrumbPath = new BreadcrumbPath(root, geometry, visibleState, null);
        this.navButtons = new NavigationButtons(geometry, expansionControls);
        this.selectionIcon = new SelectionCircleIcon(Color.BLUE, geometry.iconDiameter);
        this.blockLayout = new OutlineBlockLayout(blockCache, visibleState, geometry, nodePositioning, blockSize);
        this.focusManager = new OutlineFocusManager(this, breadcrumbPanel, outlineSelection);


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
        new OutlineActions(() -> this).installOn(this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }



    void synchronizeSelectionButton(boolean requestFocusInWindow) {
    	selectionBridge.synchronizeOutlineSelection(requestFocusInWindow);
    	focusManager.focusSelectionButton(requestFocusInWindow);
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
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);


        OutlineVisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
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
            updateFirstVisibleNodeId();
            return;
        }

        blockLayout.removeBlocksOutsideRange(this, range);
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
            boolean isInBreadcrumb = isNodeInBreadcrumbArea(hoveredNode);
            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, visibleState.getBreadcrumbAreaHeight(), nodePositioning);
            }
        }
        focusManager.restoreFocusIfNeeded(prevInOutline);
    }

    void updateVisibleBlocks(int startFromNodeIndex) {
        if (viewport == null) return;
        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);



        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        viewport.setViewPosition(startFromNodeIndex, breadcrumbAreaHeight);

        OutlineVisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        blockLayout.removeBlocksOutsideRange(this, range);
        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();


        range = viewport.calculateVisibleBlockRange(blockSize);
        lastFirstBlock = range.getFirstBlock();
        lastLastBlock = range.getLastBlock();
        lastBreadcrumbAreaHeight = range.getBreadcrumbAreaHeight();
        lastViewportWidth = viewport.getViewportWidth();
        lastVisibleNodeCount = visibleState.getVisibleNodeCount();
        updateFirstVisibleNodeId();

        TreeNode hoveredNode = visibleState.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.getChildren().isEmpty()) {
            boolean isInBreadcrumb = isNodeInBreadcrumbArea(hoveredNode);
            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, breadcrumbAreaHeight, nodePositioning);
            }
        }
        focusManager.restoreFocusIfNeeded(prevInOutline);
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

    boolean isNodeFullyVisibleInViewport(TreeNode node) {
        if (viewport == null || node == null) return false;
        int index = visibleState.findNodeIndexInVisibleList(node);
        if (index < 0) return false;
        int h = visibleState.getBreadcrumbAreaHeight();
        int first = viewport.calculateFirstVisibleNodeIndex();
        int totalRows = viewport.getPageSize();
        int breadcrumbRows = h / geometry.rowHeight;
        int contentRows = Math.max(1, totalRows - breadcrumbRows);
        int last = Math.max(first, first + contentRows - 2);
        return index >= first && index <= last;
    }

    private void createVisibleBlocks() {
        OutlineVisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        blockLayout.createVisibleBlocks(this, range, getPreferredSize().width);
    }

    private void updatePreferredFromActualBlocks() { blockLayout.updatePreferredFromActualBlocks(this); }

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
        selectionManager.select(this, outlineSelection, node, requestFocus);
    }


    OutlineSelection getOutlineSelection() {
        return outlineSelection;
    }

    int getRowHeight() {
        return geometry.rowHeight;
    }

    int calcTextButtonX(int level) {
        return geometry.calculateTextButtonX(level);
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

    void hardResetBlocksPreservingHovered(TreeNode preservedHoveredNode) {
        removeAll();
        blockCache.clear();
        navButtons.hideNavigationButtons();
        visibleState.setHoveredNode(preservedHoveredNode);
        updateVisibleBlocks();
    }

    boolean isNodeButtonFocused() { return focusManager.isNodeButtonFocused(); }

    void selectMapNodeById(String nodeId) {
    	if (selectionBridge != null)
    		selectionBridge.selectMapNodeById(nodeId);
    }

    void setSelectionBridge(OutlineSelectionBridge bridge) {
        this.selectionBridge = bridge;
        breadcrumbPanel.setSelectionBridge(bridge);
        focusManager.setSelectionBridge(bridge);
    }

    void updateNodeTitle(TreeNode node) {
        for (Component comp : breadcrumbPanel.getComponents()) {
            if (comp instanceof NodeButton) {
                NodeButton btn = (NodeButton) comp;
                if (btn.getNode() == node) {
                    btn.setText(node.getTitle());
                    int level = calculateNodeLevel(node);
                    if (level >= 0) {
                        int x = geometry.calculateTextButtonX(level);
                        btn.setBounds(x, btn.getY(), btn.getPreferredSize().width, geometry.rowHeight);
                        int rightEdge = btn.getX() + btn.getWidth();
                        blockLayout.recordButtonRightEdge(rightEdge);
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
                        int level = calculateNodeLevel(node);
                        if (level >= 0) {
                            int x = geometry.calculateTextButtonX(level);
                            btn.setBounds(x, btn.getY(), btn.getPreferredSize().width, geometry.rowHeight);
                            int rightEdge = btn.getX() + btn.getWidth();
                            blockLayout.recordButtonRightEdge(rightEdge);
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

        OutlineVisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
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

    private void removeBlocksFromBlockIndex(int startBlock) { blockLayout.removeBlocksFromBlockIndex(this, startBlock); }

    void onContentButtonHovered(TreeNode node) {
        TreeNode hoveredNode = visibleState.getHoveredNode();
        if (node != null && !node.getChildren().isEmpty() && node != hoveredNode) {

            if (isNodeInBreadcrumbArea(node)) {

                return;
            }

            visibleState.setHoveredNode(node);
            navButtons.attachToNode(node, this, false, -1, visibleState.getBreadcrumbAreaHeight(), nodePositioning);
            repaint();
        }
    }

    @Override
    public void navigateUp() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            if (currentIndex > 0) {
                TreeNode prev = visibleState.getNodeAtVisibleIndex(currentIndex - 1);
                if (prev != null) setSelectedNode(prev, true);
            }
        }
    }

	void focusSelectionButtonLater(boolean requestFocus) { focusManager.focusSelectionButtonLater(requestFocus); }

    @Override
    public void navigateDown() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int size = visibleState.getVisibleNodeCount();
            if (currentIndex >= 0 && currentIndex < size - 1) {
                TreeNode next = visibleState.getNodeAtVisibleIndex(currentIndex + 1);
                if (next != null) setSelectedNode(next, true);
            }
        }
    }

    @Override
    public void navigatePageUp() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getPageSize();
            int newIndex = Math.max(0, currentIndex - pageSize);
            if (newIndex != currentIndex) {
                TreeNode n = visibleState.getNodeAtVisibleIndex(newIndex);
                if (n != null) setSelectedNode(n, true);
            }
        }
    }

    @Override
    public void navigatePageDown() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getPageSize();
            int size = visibleState.getVisibleNodeCount();
            int newIndex = Math.min(size - 1, currentIndex + pageSize);
            if (newIndex != currentIndex) {
                TreeNode n = visibleState.getNodeAtVisibleIndex(newIndex);
                if (n != null) setSelectedNode(n, true);
            }
        }
    }

    @Override
    public void toggleExpandSelected() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null || node.childCount() == 0 || node.getLevel() == 0) return;
        if (node.isExpanded()) {
            expansionControls.collapseNode(node);
        } else {
            expansionControls.expandNode(node);
        }
    }

    @Override
    public void expandSelectedMore() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null) return;
        expansionControls.expandNodeMore(node);
    }

    @Override
    public void reduceSelectedExpansion() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null) return;
        expansionControls.reduceNodeExpansion(node);
    }

    @Override
    public void goToParent() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null) return;
        final TreeNode newSelected = node.getParent();
		if (newSelected != null) {

        	setSelectedNode(newSelected, true);
        }
    }

    @Override
    public void goToChild() {
        TreeNode node = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (node == null || node.getChildren().isEmpty()) return;
        if (!node.isExpanded()) {
            expansionControls.expandNode(node);
        }
        TreeNode targetChild = selectionManager.preferredChild(node);

        setSelectedNode(targetChild, true);
    }

    private int getPageSize() {
        if (viewport != null) {
            return viewport.getPageSize();
        }
        return 10;
    }

    void updateVisibleNodes() {
        visibleState.updateVisibleNodes();
        removeAll();
        blockCache.clear();
        navButtons.hideNavigationButtons();
        ensureSelectionVisibleTop();
    }

	void updateVisibleBlocksAndBreadcrumb() {
		Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
		boolean prevInOutline = focusManager.isWithinOutline(prevFocus);

        boolean breadcrumbAlreadyCorrect = false;
        if (viewport != null) {
            int firstIndex = viewport.calculateFirstVisibleNodeIndex();
            TreeNode firstNode = visibleState.getNodeAtVisibleIndex(firstIndex);
            if (firstNode != null) {
                List<TreeNode> crumbs = breadcrumbPanel.getCurrentBreadcrumbNodes();
                if (firstNode.getLevel() == 0) {
                    breadcrumbAlreadyCorrect = crumbs == null || crumbs.isEmpty();
                }
                else if (crumbs != null && !crumbs.isEmpty()) {
                    TreeNode lastCrumb = crumbs.get(crumbs.size() - 1);
                    breadcrumbAlreadyCorrect = (lastCrumb == firstNode.getParent()) && (crumbs.size() == firstNode.getLevel());
                }
            }
        }

        if (breadcrumbAlreadyCorrect) {
            updateVisibleBlocks();
            focusManager.restoreFocusIfNeeded(prevInOutline);
            return;
        }

		BreadcrumbState state = calculateBreadcrumbState();
	    if (state != null) {
	        breadcrumbPanel.update(state);
	        updateVisibleBlocks(state.getFirstVisibleNodeIndex());
	    }
	    else {

	        updateVisibleBlocks();
	    }
	    focusManager.restoreFocusIfNeeded(prevInOutline);
	}

    private BreadcrumbState calculateBreadcrumbState() {
        return breadcrumbPath.calculateBreadcrumbState();
    }

    void ensureSelectionVisibleTop() {
        if (viewport == null) return;
        TreeNode sel = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        int selIndex = sel != null ? visibleState.findNodeIndexInVisibleList(sel) : -1;

        BreadcrumbState state = calculateBreadcrumbState();
        if (state == null) {
            updateVisibleBlocks();
            return;
        }
        breadcrumbPanel.update(state);

        int F0 = state.getFirstVisibleNodeIndex();
        int startIndex = Math.max(0, selIndex >= 0 ? selIndex : F0);
        updateVisibleBlocks(startIndex);
        updateVisibleBlocksAndBreadcrumb();
    }



    int calculateNodeLevel(TreeNode node) {
        return nodePositioning.calculateNodeLevel(node);
    }



	void attachNavigationNode(TreeNode node,
	        boolean isBreadCrumb, int rowIndex, int currentBreadcrumbHeight) {
		visibleState.setHoveredNode(node);
		navButtons.attachToNode(node, breadcrumbPanel, isBreadCrumb, rowIndex, currentBreadcrumbHeight, nodePositioning);
	}

    boolean isNodeInBreadcrumbArea(TreeNode node) {
        List<TreeNode> crumbs = breadcrumbPanel.getCurrentBreadcrumbNodes();
        return crumbs != null && crumbs.contains(node);
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




    Collection<BlockPanel> getBlockPanels() { return blockCache.values(); }
}
