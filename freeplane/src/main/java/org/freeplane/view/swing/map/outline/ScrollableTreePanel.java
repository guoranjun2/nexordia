package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    private  OutlineViewport viewport;

    private TreeNode root;
    private final OutlineSelection outlineSelection;
    private final int blockSize;
    private VisibleOutlineNodes visibleNodes;
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
	private final OutlineDisplayMode displayMode;
	private final BreadcrumbDisplayMode breadcrumbMode = BreadcrumbDisplayMode.BY_FIRST_NODE;

    ScrollableTreePanel(OutlineDisplayMode displayMode, TreeNode root,  BreadcrumbPanel breadcrumbPanel) {
        this(displayMode, root, BLOCK_SIZE,breadcrumbPanel);
        addMouseListener(new FocusSelectedButtonClickAdapter(focusManager));
        setOpaque(true);
	}

    private ScrollableTreePanel(OutlineDisplayMode displayMode, TreeNode root, int blockSize, BreadcrumbPanel breadcrumbPanel) {
        super(null);
		this.displayMode = displayMode;
        this.root = root;
        this.blockSize = blockSize;
        this.breadcrumbPanel = breadcrumbPanel;
        this.outlineSelection = new OutlineSelection(root);
        this.visibleNodes = new VisibleOutlineNodes(root);
        this.expansionControls = new ExpansionControls(this, outlineSelection);
        this.nodePositioning = new NodePositioning(OutlineGeometry.getInstance(), visibleNodes);
        OutlineGeometry geometry = OutlineGeometry.getInstance();
        this.navButtons = new NavigationButtons(geometry, displayMode, expansionControls);
        this.blockLayout = new OutlineBlockLayout(blockCache, visibleNodes, geometry, nodePositioning, blockSize);
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

        nodePositioning.updateGeometry(geometry);
        blockLayout.updateGeometry(geometry);
        navButtons.updateGeometry(geometry);

        TreeNode hoveredNode = visibleNodes.getHoveredNode();
        String firstVisibleNodeId = visibleNodes.getFirstVisibleNodeId();
        int firstVisibleIndex = visibleNodes.findNodeIndexById(firstVisibleNodeId);
        if (firstVisibleIndex < 0) {
            firstVisibleIndex = 0;
        }
        List<TreeNode> breadcrumbNodes = breadcrumbPanel.getCurrentBreadcrumbNodes();

        removeAll();
        blockCache.clear();
        blockLayout.resetCachedMaxWidth();
        resetBlockCache();
        navButtons.hideNavigationButtons();

        visibleNodes.setHoveredNode(hoveredNode);

        if (breadcrumbNodes != null) {
            breadcrumbPanel.update(breadcrumbNodes);
        } else {
            visibleNodes.setBreadcrumbAreaHeight(0);
            breadcrumbPanel.removeAll();
            breadcrumbPanel.revalidate();
            breadcrumbPanel.repaint();
        }

        int visibleCount = visibleNodes.getVisibleNodeCount();
        if (viewport != null && visibleCount > 0) {
            int clampedIndex = Math.min(firstVisibleIndex, visibleCount - 1);
            updateVisibleBlocks(clampedIndex);
        } else {
            revalidate();
            repaint();
        }
    }

    @SuppressWarnings("unused")
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
        TreeNode hoveredNode = visibleNodes.getHoveredNode();
        reattachNavigationButtons(hoveredNode);

        revalidate();
        repaint();
    }

    private void reattachNavigationButtons(TreeNode hoveredNode) {
        if (hoveredNode == null || hoveredNode.getChildren().isEmpty()) {
            return;
        }

        boolean inBreadcrumb = isNodeInBreadcrumbArea(hoveredNode);
        int breadcrumbHeight = visibleNodes.getBreadcrumbAreaHeight();
        if (inBreadcrumb) {
            List<TreeNode> breadcrumbNodes = breadcrumbPanel.getCurrentBreadcrumbNodes();
            int rowIndex = breadcrumbNodes.indexOf(hoveredNode);
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
        this.viewport = new OutlineViewport(scroll, visibleNodes, nodePositioning);
        resetBlockCache();
    }

    void setBreadcrumbAreaHeight(int height) {
        this.visibleNodes.setBreadcrumbAreaHeight(height);
    }



        void updateVisibleBlocks() {
        if (viewport == null) return;
        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);


        OutlineVisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        int viewportWidth = viewport.getViewportWidth();
        int visibleCount = visibleNodes.getVisibleNodeCount();
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

        TreeNode hoveredNode = visibleNodes.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.getChildren().isEmpty()) {
            boolean isInBreadcrumb = isNodeInBreadcrumbArea(hoveredNode);
            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, visibleNodes.getBreadcrumbAreaHeight(), nodePositioning);
            }
        }
        focusManager.restoreFocusIfNeeded(prevInOutline);
    }

    void updateVisibleBlocks(int startFromNodeIndex) {
        if (viewport == null) return;
        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);



        int breadcrumbAreaHeight = visibleNodes.getBreadcrumbAreaHeight();
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
        lastVisibleNodeCount = visibleNodes.getVisibleNodeCount();
        updateFirstVisibleNodeId();

        TreeNode hoveredNode = visibleNodes.getHoveredNode();
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
        int index = visibleNodes.findNodeIndexInVisibleList(node);
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
        int count = visibleNodes.getVisibleNodeCount();
        if (count == 0) {
            visibleNodes.setFirstVisibleNodeId(null);
            return;
        }
        index = Math.max(0, Math.min(index, count - 1));
        visibleNodes.setFirstVisibleNodeId(visibleNodes.getNodeIdAtVisibleIndex(index));
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
        return OutlineGeometry.getInstance().calculateNodeButtonX(displayMode.showsNavigationButtons(), level);
    }

    int getViewportWidth() {
        return viewport != null ? viewport.getViewportWidth() : getWidth();
    }

    VisibleOutlineNodes getVisibleNodes() {
        return visibleNodes;
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
        visibleNodes.setHoveredNode(preservedHoveredNode);
        updateVisibleBlocks();
    }

    boolean isNodeButtonFocused() { return focusManager.isNodeButtonFocused(); }

    void selectMapNodeById(String nodeId) {
    	if (selectionBridge != null)
    		selectionBridge.selectMapNodeById(nodeId);
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
                        int x = OutlineGeometry.getInstance().calculateNodeButtonX(displayMode.showsNavigationButtons(), level);
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
                            int x = OutlineGeometry.getInstance().calculateNodeButtonX(displayMode.showsNavigationButtons(), level);
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

        visibleNodes.updateVisibleNodes();
		int firstFullyVisibleNodeIndex = viewport.calculateFirstVisibleNodeIndex();

        List<TreeNode> state = calculateBreadcrumbStateForIndex(firstFullyVisibleNodeIndex);

        TreeNode preservedHovered = visibleNodes.getHoveredNode();
        removeAll();
        blockCache.clear();
        blockLayout.resetCachedMaxWidth();
        navButtons.hideNavigationButtons();
        visibleNodes.setHoveredNode(preservedHovered);

        if (state != null) {
            breadcrumbPanel.update(state);
            updateVisibleBlocks(firstFullyVisibleNodeIndex);

            TreeNode hovered = visibleNodes.getHoveredNode();
            if (hovered != null && visibleNodes.findNodeIndexInVisibleList(hovered) < 0) {
                visibleNodes.setHoveredNode(null);
            }

            ensureValidSelectionOrSyncFromMap();
            focusManager.restoreFocusIfNeeded(prevInOutline);
            return;
        }

        int anchorIndex = visibleNodes.findNodeIndexInVisibleList(anchorNode);
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
        visibleNodes.setHoveredNode(preservedHovered);
        updateVisibleBlocks();

        TreeNode hovered = visibleNodes.getHoveredNode();
        if (hovered != null && visibleNodes.findNodeIndexInVisibleList(hovered) < 0) {
            visibleNodes.setHoveredNode(null);
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

    void onContentButtonHovered(TreeNode node) {
        TreeNode hoveredNode = visibleNodes.getHoveredNode();
        if (node != null && !node.getChildren().isEmpty() && node != hoveredNode) {

            if (isNodeInBreadcrumbArea(node)) {

                return;
            }

            visibleNodes.setHoveredNode(node);
            navButtons.attachToNode(node, this, false, -1, visibleNodes.getBreadcrumbAreaHeight(), nodePositioning);
            repaint();
        }
    }

    @Override
    public void navigateUp() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleNodes.findNodeIndexInVisibleList(currentSelected);
            if (currentIndex > 0) {
                TreeNode prev = visibleNodes.getNodeAtVisibleIndex(currentIndex - 1);
                if (prev != null) setSelectedNode(prev, true);
            }
        }
    }

	void focusSelectionButtonLater(boolean requestFocus) { focusManager.focusSelectionButtonLater(requestFocus); }

    @Override
    public void navigateDown() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected == null || viewport == null) return;

        int currentIndex = visibleNodes.findNodeIndexInVisibleList(currentSelected);
        int size = visibleNodes.getVisibleNodeCount();
        if (currentIndex < 0 || currentIndex >= size - 1) return;

        int nextIndex = currentIndex + 1;
        TreeNode nextNode = visibleNodes.getNodeAtVisibleIndex(nextIndex);
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
		List<TreeNode> planned = calculateBreadcrumbStateForIndex(targetFirstIndex);
        if (planned != null) {
            breadcrumbPanel.update(planned);
            updateVisibleBlocks(targetFirstIndex);
        }

        setSelectedNode(nextNode, true);
    }

    private List<TreeNode> calculateBreadcrumbStateForIndex(int firstVisibleNodeIndex) {
        TreeNode breadcrumbTargetNode = visibleNodes.getNodeAtVisibleIndex(firstVisibleNodeIndex);
        if(breadcrumbTargetNode == null)
			return null;
		List<TreeNode> newBreadcrumbNodes = collectBreadcrumbNodes(breadcrumbTargetNode);
		return newBreadcrumbNodes;
    }

    private List<TreeNode> calculateBreadcrumbStateForSelection() {
        TreeNode breadcrumbTargetNode = outlineSelection.getSelectedNode();
        if(breadcrumbTargetNode == null)
			return null;
		List<TreeNode> newBreadcrumbNodes = collectBreadcrumbNodes(breadcrumbTargetNode);
		newBreadcrumbNodes.add(breadcrumbTargetNode);
		return newBreadcrumbNodes;
    }

    private List<TreeNode> collectBreadcrumbNodes(TreeNode fromNode) {
    	List<TreeNode> breadcrumbNodes = new ArrayList<>();
        TreeNode current = fromNode.getParent();
        while (current != null) {
            breadcrumbNodes.add(current);
            current = current.getParent();
        }
        Collections.reverse(breadcrumbNodes);
        return breadcrumbNodes;
    }


    @Override
    public void navigatePageUp() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleNodes.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getContentRows();
            int size = visibleNodes.getVisibleNodeCount();
            int currentFirstVisibleIndex = viewport != null ? viewport.calculateFirstVisibleNodeIndex() : 0;
            int lastVisibleIndex = Math.min(size - 1, currentFirstVisibleIndex + pageSize - 1);
            if (currentIndex > currentFirstVisibleIndex) {
                TreeNode firstVisibleNode = visibleNodes.getNodeAtVisibleIndex(currentFirstVisibleIndex);
                if (firstVisibleNode != null) setSelectedNode(firstVisibleNode, true);
                updateVisibleBlocksAndBreadcrumb();
                return;
            }
            int tentativeFirstIndex = Math.max(0, currentFirstVisibleIndex - pageSize + 1); // overlap = 1
            int breadcrumbRowsAtTentative = getNodeLevelAtVisibleIndex(tentativeFirstIndex);
            int contentRowCountAfterTentative = getContentRowsForBreadcrumbRows(breadcrumbRowsAtTentative);
            int maxFeasibleFirstIndex = Math.max(0, size - contentRowCountAfterTentative);
            int targetFirstIndex = Math.min(tentativeFirstIndex, maxFeasibleFirstIndex);
            List<TreeNode> planned = calculateBreadcrumbStateForIndex(targetFirstIndex);
            if (planned != null) {
                breadcrumbPanel.update(planned);
                updateVisibleBlocks(targetFirstIndex);
            }
            TreeNode newFirstNode = visibleNodes.getNodeAtVisibleIndex(targetFirstIndex);
            if (newFirstNode != null) setSelectedNode(newFirstNode, true);
        }
    }

    @Override
    public void navigatePageDown() {
        TreeNode currentSelected = outlineSelection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleNodes.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getContentRows();
            int size = visibleNodes.getVisibleNodeCount();

            int currentFirstVisibleIndex = viewport != null ? viewport.calculateFirstVisibleNodeIndex() : 0;
            int lastVisibleIndex = Math.min(size - 1, currentFirstVisibleIndex + pageSize - 1);
            if (currentIndex < lastVisibleIndex) {
                TreeNode lastVisibleNode = visibleNodes.getNodeAtVisibleIndex(lastVisibleIndex);
                if (lastVisibleNode != null) setSelectedNode(lastVisibleNode, true);
                updateVisibleBlocksAndBreadcrumb();
                return;
            }
            int tentativeFirstIndex = Math.min(size - 1, currentFirstVisibleIndex + pageSize - 1); // overlap = 1
            int breadcrumbRowsAtTentative = getNodeLevelAtVisibleIndex(tentativeFirstIndex);
            int contentRowCountAfterTentative = getContentRowsForBreadcrumbRows(breadcrumbRowsAtTentative);
            int maxFeasibleFirstIndex = Math.max(0, size - contentRowCountAfterTentative);
            int targetFirstIndex = Math.min(tentativeFirstIndex, maxFeasibleFirstIndex);
            List<TreeNode> planned = calculateBreadcrumbStateForIndex(targetFirstIndex);
            if (planned != null) {
                breadcrumbPanel.update(planned);
                updateVisibleBlocks(targetFirstIndex);
            }

            int newLastVisibleIndex = Math.min(size - 1, targetFirstIndex + contentRowCountAfterTentative - 1);
            TreeNode newLastVisibleNode = visibleNodes.getNodeAtVisibleIndex(newLastVisibleIndex);
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
            int breadcrumbAreaHeight = visibleNodes.getBreadcrumbAreaHeight();
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
        TreeNode node = visibleNodes.getNodeAtVisibleIndex(visibleIndex);
        return node != null ? node.getLevel() : 0;
    }

    void updateVisibleNodes() {
        visibleNodes.updateVisibleNodes();
        removeAll();
        blockCache.clear();
        navButtons.hideNavigationButtons();
        ensureSelectionVisibleTop();
    }

    void updateVisibleBlocksAndBreadcrumb() {
        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);
		int firstFullyVisibleNodeIndex = viewport.calculateFirstVisibleNodeIndex();
        List<TreeNode> state = calculateBreadcrumbStateForIndex(firstFullyVisibleNodeIndex);
        if (state != null) {
            int oldBreadcrumbHeight = visibleNodes.getBreadcrumbAreaHeight();
            breadcrumbPanel.update(state);
            int newBreadcrumbHeight = visibleNodes.getBreadcrumbAreaHeight();
            if (newBreadcrumbHeight != oldBreadcrumbHeight) {
                TreeNode preservedHovered = visibleNodes.getHoveredNode();
                hardResetBlocksPreservingHovered(preservedHovered);
            }
            updateVisibleBlocks(firstFullyVisibleNodeIndex);
        }
        else {
            updateVisibleBlocks();
        }
        focusManager.restoreFocusIfNeeded(prevInOutline);
    }

    void ensureSelectionVisibleTop() {
        if (viewport == null) return;
        TreeNode selected = outlineSelection != null ? outlineSelection.getSelectedNode() : null;
        int selectedIndex = selected != null ? visibleNodes.findNodeIndexInVisibleList(selected) : -1;

        int first = viewport.calculateFirstVisibleNodeIndex();
        int currentBreadcrumbRows = getNodeLevelAtVisibleIndex(first);
        int contentRows = getContentRowsForBreadcrumbRows(currentBreadcrumbRows);
        int last = Math.max(first, first + contentRows - 1);
        boolean haveButtons = getComponentCount() > 0 || !blockCache.isEmpty();
        if (!haveButtons && contentRows <= 1) {
            updateVisibleBlocks();
            return;
        }

        int visibleCount = visibleNodes.getVisibleNodeCount();
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
        List<TreeNode> planned = calculateBreadcrumbStateForIndex(targetFirst);
        if (planned != null) {
            int oldBreadcrumbHeight = visibleNodes.getBreadcrumbAreaHeight();
            breadcrumbPanel.update(planned);
            int newBreadcrumbHeight = visibleNodes.getBreadcrumbAreaHeight();
            if (newBreadcrumbHeight != oldBreadcrumbHeight) {
                TreeNode preservedHovered = visibleNodes.getHoveredNode();
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
		visibleNodes.setHoveredNode(node);
		navButtons.attachToNode(node, breadcrumbPanel, isBreadCrumb, rowIndex, currentBreadcrumbHeight, nodePositioning);
	}

    boolean isNodeInBreadcrumbArea(TreeNode node) {
        List<TreeNode> crumbs = breadcrumbPanel.getCurrentBreadcrumbNodes();
        return crumbs != null && crumbs.contains(node);
    }

    boolean areNavButtonsVisible() {
        return navButtons.expandBtn.isVisible() || navButtons.collapseBtn.isVisible()
                || navButtons.expandMoreBtn.isVisible() || navButtons.reduceBtn.isVisible();
    }




    Collection<BlockPanel> getBlockPanels() { return blockCache.values(); }

	OutlineDisplayMode getDisplayMode() {
		return displayMode;
	}
}
