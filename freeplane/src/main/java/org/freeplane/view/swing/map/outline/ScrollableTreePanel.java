package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.freeplane.core.resources.IFreeplanePropertyListener;
import org.freeplane.core.resources.ResourceController;

class ScrollableTreePanel extends JPanel implements OutlineActionTarget {
	private static final long serialVersionUID = 1;
    private static final int BLOCK_SIZE = 50;



	private  final NavigationButtons navButtons;
    private final BreadcrumbPanel breadcrumbPanel;
    private final ExpansionControls expansionControls;
    private NodePositioning nodePositioning;
    private  final BreadcrumbPath breadcrumbPath;
    private  OutlineViewport viewport;

    private TreeNode root;
    private final OutlineSelection outlineSelection;
    private final int blockSize;
    private VisibleOutlineState visibleState;
    private OutlineGeometry currentGeometry;
    private final OutlineGeometry.GeometryListener geometryListener;
    private boolean geometryListenerRegistered;
    private final IFreeplanePropertyListener outlinePropertyListener;
    private boolean outlinePropertyListenerRegistered;
    private final OutlineBlockViewCache blockCache = new OutlineBlockViewCache();
    private OutlineBlockLayout blockLayout;
    private OutlineSelectionBridge selectionBridge;
    private OutlineFocusManager focusManager;
    private final OutlineSelectionManager selectionManager = new OutlineSelectionManager();
    private Supplier<Color> backgroundColorSupplier;

    private int lastFirstBlock = -1;
    private int lastLastBlock = -1;
    private int lastBreadcrumbAreaHeight = -1;
    private int lastViewportWidth = -1;
    private int lastVisibleNodeCount = -1;

    ScrollableTreePanel(TreeNode root,  BreadcrumbPanel breadcrumbPanel) {
        this(root, BLOCK_SIZE,breadcrumbPanel);
        addMouseListener(new FocusSelectedButtonClickAdapter(focusManager));
        setOpaque(true);
	}

    private ScrollableTreePanel(TreeNode root, int blockSize, BreadcrumbPanel breadcrumbPanel) {
        super(null);
        this.root = root;
        this.blockSize = blockSize;
        this.breadcrumbPanel = breadcrumbPanel;
        this.outlineSelection = new OutlineSelection(root);
        this.visibleState = new VisibleOutlineState(root);
        this.expansionControls = new ExpansionControls(this, outlineSelection);
        this.nodePositioning = new NodePositioning(OutlineGeometry.getInstance(), visibleState);
        OutlineGeometry geometry = OutlineGeometry.getInstance();
        this.currentGeometry = geometry;
        this.breadcrumbPath = new BreadcrumbPath(root, geometry, visibleState, null);
        this.navButtons = new NavigationButtons(geometry, expansionControls);
        this.blockLayout = new OutlineBlockLayout(blockCache, visibleState, geometry, nodePositioning, blockSize);
        this.focusManager = new OutlineFocusManager(this, breadcrumbPanel, outlineSelection);
        this.geometryListener = this::handleGeometryChange;
        this.outlinePropertyListener = this::handleOutlinePropertyChange;


        setFocusable(true);
        setupKeyBindings();

        add(navButtons.expandBtn);
        add(navButtons.collapseBtn);
        add(navButtons.expandMoreBtn);
        add(navButtons.reduceBtn);

        navButtons.hideNavigationButtons();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (!geometryListenerRegistered) {
            OutlineGeometry.registerListener(geometryListener);
            geometryListenerRegistered = true;
        }
        if (!outlinePropertyListenerRegistered) {
            ResourceController.getResourceController().addPropertyChangeListener(outlinePropertyListener);
            outlinePropertyListenerRegistered = true;
        }
    }

    @Override
    public void removeNotify() {
        if (geometryListenerRegistered) {
            OutlineGeometry.unregisterListener(geometryListener);
            geometryListenerRegistered = false;
        }
        if (outlinePropertyListenerRegistered) {
            ResourceController.getResourceController().removePropertyChangeListener(outlinePropertyListener);
            outlinePropertyListenerRegistered = false;
        }
        super.removeNotify();
    }

    private void handleGeometryChange(OutlineGeometry geometry) {
        if (geometry == null) {
            return;
        }

        currentGeometry = geometry;
        nodePositioning.updateGeometry(geometry);
        blockLayout.updateGeometry(geometry);
        breadcrumbPath.updateGeometry(geometry);
        navButtons.updateGeometry(geometry);

        TreeNode hoveredNode = visibleState.getHoveredNode();
        String firstVisibleNodeId = visibleState.getFirstVisibleNodeId();
        int firstVisibleIndex = visibleState.findNodeIndexById(firstVisibleNodeId);
        if (firstVisibleIndex < 0) {
            firstVisibleIndex = 0;
        }
        List<TreeNode> breadcrumbNodes = breadcrumbPanel.getCurrentBreadcrumbNodes();

        removeAll();
        blockCache.clear();
        blockLayout.resetCachedMaxWidth();
        resetBlockCache();
        navButtons.hideNavigationButtons();

        visibleState.setHoveredNode(hoveredNode);

        if (breadcrumbNodes != null) {
            BreadcrumbState restoredState = new BreadcrumbState(breadcrumbNodes,
                    breadcrumbNodes.size() * geometry.rowHeight,
                    firstVisibleIndex);
            breadcrumbPanel.update(restoredState);
        } else {
            visibleState.setBreadcrumbAreaHeight(0);
            breadcrumbPanel.removeAll();
            breadcrumbPanel.revalidate();
            breadcrumbPanel.repaint();
        }

        int visibleCount = visibleState.getVisibleNodeCount();
        if (viewport != null && visibleCount > 0) {
            int clampedIndex = Math.min(firstVisibleIndex, visibleCount - 1);
            updateVisibleBlocks(clampedIndex);
        } else {
            revalidate();
            repaint();
        }
    }

    private void handleOutlinePropertyChange(String propertyName, String newValue, String oldValue) {
        if (!"useColoredOutlineItems".equals(propertyName)) {
            return;
        }

        refreshColoredOutlineItems();
    }

    private void refreshColoredOutlineItems() {
        for (BlockPanel panel : blockCache.values()) {
            panel.rebuildNodeButtons();
        }
        blockLayout.recomputeCachedMaxWidth();
        blockLayout.updatePreferredFromActualBlocks(this);

        breadcrumbPanel.updateNodeButtons();

        navButtons.hideNavigationButtons();
        TreeNode hoveredNode = visibleState.getHoveredNode();
        reattachNavigationButtons(hoveredNode);

        revalidate();
        repaint();
    }

    private void reattachNavigationButtons(TreeNode hoveredNode) {
        if (hoveredNode == null || hoveredNode.getChildren().isEmpty()) {
            return;
        }

        boolean inBreadcrumb = isNodeInBreadcrumbArea(hoveredNode);
        int breadcrumbHeight = visibleState.getBreadcrumbAreaHeight();
        if (inBreadcrumb) {
            List<TreeNode> breadcrumbNodes = breadcrumbPanel.getCurrentBreadcrumbNodes();
            int rowIndex = findNodeIndexInBreadcrumbPath(hoveredNode, breadcrumbNodes);
            if (rowIndex >= 0) {
                navButtons.attachToNode(hoveredNode, breadcrumbPanel, true, rowIndex, breadcrumbHeight, nodePositioning);
            }
            return;
        }

        navButtons.attachToNode(hoveredNode, this, false, -1, breadcrumbHeight, nodePositioning);
    }

    @SuppressWarnings("serial")
    private void setupKeyBindings() {
        new OutlineActions(() -> this).installOn(this, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

	void setBackgroundColorSupplier(Supplier<Color> backgroundColorSupplier) {
		this.backgroundColorSupplier = backgroundColorSupplier;
	}

    @Override
	public Color getBackground() {
		 if (backgroundColorSupplier != null) {
			final Color suppliedColor = backgroundColorSupplier.get();
			if(suppliedColor != null)
				return suppliedColor;
		 }
		 return super.getBackground();
	}


    void synchronizeSelectionButton(boolean requestFocusInWindow) {
    	selectionBridge.synchronizeOutlineSelection(requestFocusInWindow);
    	focusManager.focusSelectionButtonLater(requestFocusInWindow);
    }





    void setScrollPane(JScrollPane scroll) {
        this.viewport = new OutlineViewport(scroll, visibleState, nodePositioning);
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

    boolean isNodeFullyVisibleInViewport(TreeNode node) {
        if (viewport == null || node == null) return false;
        int index = visibleState.findNodeIndexInVisibleList(node);
        if (index < 0) return false;
        int first = viewport.calculateFirstVisibleNodeIndex();
        int currentBreadcrumbRows = getNodeLevelAtVisibleIndex(first);
        int contentRows = getContentRowsForBreadcrumbRows(currentBreadcrumbRows);
        int last = Math.max(first, first + contentRows - 1);
        return index >= first && index <= last;
    }

    private void createVisibleBlocks() {
        OutlineVisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        blockLayout.createVisibleBlocks(this, range, getWidth());
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
        return OutlineGeometry.getInstance().rowHeight;
    }

    int calcTextButtonX(int level) {
        return OutlineGeometry.getInstance().calculateNodeButtonX(level);
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
    	if (selectionBridge != null) {
			selectionBridge.selectMapNodeById(nodeId);
		}
    }

    @Override
	public void selectSelectedInMap() {
        TreeNode selected = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (selected != null) {
            selectMapNodeById(selected.getId());
        }
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
                    btn.updateLabel();
                    int level = calculateNodeLevel(node);
                    if (level >= 0) {
                        int x = OutlineGeometry.getInstance().calculateNodeButtonX(level);
                        btn.setBounds(x, btn.getY(), btn.getPreferredSize().width, OutlineGeometry.getInstance().rowHeight);
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
                        btn.updateLabel();
                        int level = calculateNodeLevel(node);
                        if (level >= 0) {
                            int x = OutlineGeometry.getInstance().calculateNodeButtonX(level);
                            btn.setBounds(x, btn.getY(), btn.getPreferredSize().width, OutlineGeometry.getInstance().rowHeight);
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

        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);

        navButtons.hideNavigationButtons();

        visibleState.updateVisibleNodes();

        BreadcrumbState state = calculateBreadcrumbState();

        TreeNode preservedHovered = visibleState.getHoveredNode();
        removeAll();
        blockCache.clear();
        blockLayout.resetCachedMaxWidth();
        navButtons.hideNavigationButtons();
        visibleState.setHoveredNode(preservedHovered);

        if (state != null) {
            breadcrumbPanel.update(state);
            updateVisibleBlocks(state.getFirstVisibleNodeIndex());

            TreeNode hovered = visibleState.getHoveredNode();
            if (hovered != null && visibleState.findNodeIndexInVisibleList(hovered) < 0) {
                visibleState.setHoveredNode(null);
            }

            ensureValidSelectionOrSyncFromMap();
            focusManager.restoreFocusIfNeeded(prevInOutline);
            return;
        }

        int anchorIndex = visibleState.findNodeIndexInVisibleList(anchorNode);
        if (anchorIndex < 0) {
            updateVisibleBlocksAndBreadcrumb();
            ensureValidSelectionOrSyncFromMap();
            focusManager.restoreFocusIfNeeded(prevInOutline);
            return;
        }

        removeAll();
        blockCache.clear();
        blockLayout.resetCachedMaxWidth();
        navButtons.hideNavigationButtons();
        visibleState.setHoveredNode(preservedHovered);
        updateVisibleBlocks();

        TreeNode hovered = visibleState.getHoveredNode();
        if (hovered != null && visibleState.findNodeIndexInVisibleList(hovered) < 0) {
            visibleState.setHoveredNode(null);
        }

        ensureValidSelectionOrSyncFromMap();
        focusManager.restoreFocusIfNeeded(prevInOutline);
    }

    private void ensureValidSelectionOrSyncFromMap() {
        TreeNode selected = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        if (selected == null) return;
        if (!isNodeAttachedToRoot(selected)) {
            if (selectionBridge != null) {
                selectionBridge.synchronizeOutlineSelection(false);
            }
        }
    }

    private boolean isNodeAttachedToRoot(TreeNode node) {
        for (TreeNode n = node; n != null; n = n.getParent()) {
            if (n == root) return true;
        }
        return false;
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
        if (currentSelected == null || viewport == null) return;

        int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
        int size = visibleState.getVisibleNodeCount();
        if (currentIndex < 0 || currentIndex >= size - 1) return;

        int nextIndex = currentIndex + 1;
        TreeNode nextNode = visibleState.getNodeAtVisibleIndex(nextIndex);
        if (nextNode == null) return;
        if (isNodeInBreadcrumbArea(nextNode)) {
            setSelectedNode(nextNode, true);
            return;
        }
        if (isNodeFullyVisibleInViewport(nextNode)) {
            setSelectedNode(nextNode, true);
            return;
        }
        int currentFirstVisibleIndex = viewport.calculateFirstVisibleNodeIndex();
        int currentBreadcrumbRowCount = getNodeLevelAtVisibleIndex(currentFirstVisibleIndex);
        int nextBreadcrumbRowCount = getNodeLevelAtVisibleIndex(Math.min(currentFirstVisibleIndex + 1, size - 1));
        int breadcrumbRowDelta = nextBreadcrumbRowCount - currentBreadcrumbRowCount; // изменение высоты крошек при продвижении верхнего на 1
        int minimalScrollRows = Math.max(0, 1 + breadcrumbRowDelta);

        int tentativeFirstIndex = Math.min(currentFirstVisibleIndex + minimalScrollRows, size - 1);
        int breadcrumbRowsAtTentative = getNodeLevelAtVisibleIndex(tentativeFirstIndex);
        int contentRowCountAfterTentative = getContentRowsForBreadcrumbRows(breadcrumbRowsAtTentative);
        int maxFeasibleFirstIndex = Math.max(0, size - contentRowCountAfterTentative);
        int targetFirstIndex = Math.min(tentativeFirstIndex, maxFeasibleFirstIndex);

        BreadcrumbState planned = breadcrumbPath.calculateBreadcrumbStateForIndex(targetFirstIndex);
        if (planned != null) {
            breadcrumbPanel.update(planned);
            updateVisibleBlocks(targetFirstIndex);
        }

        setSelectedNode(nextNode, true);
    }

    @Override
    public void navigatePageUp() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getContentRows();
            int size = visibleState.getVisibleNodeCount();
            int currentFirstVisibleIndex = viewport != null ? viewport.calculateFirstVisibleNodeIndex() : 0;
            int lastVisibleIndex = Math.min(size - 1, currentFirstVisibleIndex + pageSize - 1);
            if (currentIndex > currentFirstVisibleIndex) {
                TreeNode firstVisibleNode = visibleState.getNodeAtVisibleIndex(currentFirstVisibleIndex);
                if (firstVisibleNode != null) setSelectedNode(firstVisibleNode, true);
                updateVisibleBlocksAndBreadcrumb();
                return;
            }
            int tentativeFirstIndex = Math.max(0, currentFirstVisibleIndex - pageSize + 1); // overlap = 1
            int breadcrumbRowsAtTentative = getNodeLevelAtVisibleIndex(tentativeFirstIndex);
            int contentRowCountAfterTentative = getContentRowsForBreadcrumbRows(breadcrumbRowsAtTentative);
            int maxFeasibleFirstIndex = Math.max(0, size - contentRowCountAfterTentative);
            int targetFirstIndex = Math.min(tentativeFirstIndex, maxFeasibleFirstIndex);
            BreadcrumbState planned = breadcrumbPath.calculateBreadcrumbStateForIndex(targetFirstIndex);
            if (planned != null) {
                breadcrumbPanel.update(planned);
                updateVisibleBlocks(targetFirstIndex);
            }
            TreeNode newFirstNode = visibleState.getNodeAtVisibleIndex(targetFirstIndex);
            if (newFirstNode != null) setSelectedNode(newFirstNode, true);
        }
    }

    @Override
    public void navigatePageDown() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getContentRows();
            int size = visibleState.getVisibleNodeCount();

            int currentFirstVisibleIndex = viewport != null ? viewport.calculateFirstVisibleNodeIndex() : 0;
            int lastVisibleIndex = Math.min(size - 1, currentFirstVisibleIndex + pageSize - 1);
            if (currentIndex < lastVisibleIndex) {
                TreeNode lastVisibleNode = visibleState.getNodeAtVisibleIndex(lastVisibleIndex);
                if (lastVisibleNode != null) setSelectedNode(lastVisibleNode, true);
                updateVisibleBlocksAndBreadcrumb();
                return;
            }
            int tentativeFirstIndex = Math.min(size - 1, currentFirstVisibleIndex + pageSize - 1); // overlap = 1
            int breadcrumbRowsAtTentative = getNodeLevelAtVisibleIndex(tentativeFirstIndex);
            int contentRowCountAfterTentative = getContentRowsForBreadcrumbRows(breadcrumbRowsAtTentative);
            int maxFeasibleFirstIndex = Math.max(0, size - contentRowCountAfterTentative);
            int targetFirstIndex = Math.min(tentativeFirstIndex, maxFeasibleFirstIndex);
            BreadcrumbState planned = breadcrumbPath.calculateBreadcrumbStateForIndex(targetFirstIndex);
            if (planned != null) {
                breadcrumbPanel.update(planned);
                updateVisibleBlocks(targetFirstIndex);
            }

            int newLastVisibleIndex = Math.min(size - 1, targetFirstIndex + contentRowCountAfterTentative - 1);
            TreeNode newLastVisibleNode = visibleState.getNodeAtVisibleIndex(newLastVisibleIndex);
            if (newLastVisibleNode != null) setSelectedNode(newLastVisibleNode, true);
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

    private int getContentRows() {
        if (viewport != null) {
            int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
            int viewportHeight = viewport.getViewportHeight() - breadcrumbAreaHeight;
            return Math.max(1, viewportHeight / OutlineGeometry.getInstance().rowHeight - 1);
        }
        return 10;
    }

    private int getContentRowsForBreadcrumbRows(int breadcrumbRowCount) {
        if (viewport != null) {
            int viewportHeight = viewport.getViewportHeight() - breadcrumbRowCount * OutlineGeometry.getInstance().rowHeight;
            return Math.max(1, viewportHeight / OutlineGeometry.getInstance().rowHeight - 1);
        }
        return 10;
    }

    private int getNodeLevelAtVisibleIndex(int visibleIndex) {
        TreeNode node = visibleState.getNodeAtVisibleIndex(visibleIndex);
        return node != null ? node.getLevel() : 0;
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
        BreadcrumbState state = calculateBreadcrumbState();
        if (state != null) {
            int oldBreadcrumbHeight = visibleState.getBreadcrumbAreaHeight();
            breadcrumbPanel.update(state);
            int newBreadcrumbHeight = visibleState.getBreadcrumbAreaHeight();
            if (newBreadcrumbHeight != oldBreadcrumbHeight) {
                TreeNode preservedHovered = visibleState.getHoveredNode();
                hardResetBlocksPreservingHovered(preservedHovered);
            }
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
        TreeNode selected = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        int selectedIndex = selected != null ? visibleState.findNodeIndexInVisibleList(selected) : -1;

        int first = viewport.calculateFirstVisibleNodeIndex();
        int currentBreadcrumbRows = getNodeLevelAtVisibleIndex(first);
        int contentRows = getContentRowsForBreadcrumbRows(currentBreadcrumbRows);
        int last = Math.max(first, first + contentRows - 1);
        boolean haveButtons = getComponentCount() > 0 || !blockCache.isEmpty();
        if (!haveButtons && contentRows <= 1) {
            updateVisibleBlocks();
            return;
        }

        int visibleCount = visibleState.getVisibleNodeCount();
        if (haveButtons) {
            if (selected == null || selectedIndex < 0) return;
            if (isNodeInBreadcrumbArea(selected)) return;
            if (selectedIndex >= first && selectedIndex <= last) return;
        }
        int targetFirst;
        if (selected == null || selectedIndex < 0) {
            targetFirst = first;
        }
        else if (isNodeInBreadcrumbArea(selected)) {
            targetFirst = first;
        }
        else if (selectedIndex < first) {
            int desiredFirst = Math.max(0, selectedIndex);
            int br = getNodeLevelAtVisibleIndex(desiredFirst);
            int cr = getContentRowsForBreadcrumbRows(br);
            int maxFeasibleFirst = Math.max(0, visibleCount - cr);
            targetFirst = Math.min(desiredFirst, maxFeasibleFirst);
        }
        else if (selectedIndex > last) {
            int desiredFirst = Math.max(0, selectedIndex - (contentRows - 1));
            int br = getNodeLevelAtVisibleIndex(desiredFirst);
            int cr = getContentRowsForBreadcrumbRows(br);
            int maxFeasibleFirst = Math.max(0, visibleCount - cr);
            targetFirst = Math.min(desiredFirst, maxFeasibleFirst);
        }
        else {
            targetFirst = first;
        }

        BreadcrumbState planned = breadcrumbPath.calculateBreadcrumbStateForIndex(targetFirst);
        if (planned != null) {
            int oldBreadcrumbHeight = visibleState.getBreadcrumbAreaHeight();
            breadcrumbPanel.update(planned);
            int newBreadcrumbHeight = visibleState.getBreadcrumbAreaHeight();
            if (newBreadcrumbHeight != oldBreadcrumbHeight) {
                TreeNode preservedHovered = visibleState.getHoveredNode();
                hardResetBlocksPreservingHovered(preservedHovered);
            }
            updateVisibleBlocks(targetFirst);
        }
        else {
            updateVisibleBlocks();
        }
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

    boolean areNavButtonsVisible() {
        return navButtons.expandBtn.isVisible() || navButtons.collapseBtn.isVisible()
                || navButtons.expandMoreBtn.isVisible() || navButtons.reduceBtn.isVisible();
    }




    Collection<BlockPanel> getBlockPanels() { return blockCache.values(); }
}
