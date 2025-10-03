package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
    private final JPanel blockPanel;

    private TreeNode root;
    private final OutlineSelection outlineSelection;
    private final int blockSize;
    private final VisibleOutlineNodes visibleNodes;
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
    private int lastBreadcrumbHeight = -1;
    private int lastViewportWidth = -1;
    private int lastVisibleNodeCount = -1;
	private final OutlineDisplayMode displayMode;
	private BreadcrumbMode breadcrumbMode;

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
        this.breadcrumbMode = ResourceController.getResourceController().getEnumProperty("outlineBreadcrumbMode", BreadcrumbMode.DEFAULT);
        this.outlineSelection = new OutlineSelection(root);
        this.visibleNodes = new VisibleOutlineNodes(root);
        this.expansionControls = new ExpansionControls(this, outlineSelection);
        OutlineGeometry geometry = OutlineGeometry.getInstance();
        this.nodePositioning = new NodePositioning(geometry, visibleNodes,calculateDuplicateItemsHeight());
        this.navButtons = new NavigationButtons(geometry, displayMode, expansionControls);
        this.blockPanel = new JPanel(null);
        blockPanel.setOpaque(false);
        add(blockPanel);
        this.blockLayout = new OutlineBlockLayout(blockCache, visibleNodes, geometry, nodePositioning, blockSize, this);
        this.focusManager = new OutlineFocusManager(this, breadcrumbPanel, outlineSelection);
        this.geometryListener = this::handleGeometryChange;
        this.outlinePropertyListener = this::handleOutlinePropertyChange;
        setFocusable(true);
        setupKeyBindings();
        navButtons.hideNavigationButtons();
    }

	private int calculateDuplicateItemsHeight() {
		return (isSelectionDrivenBreadcrumbMode() ? OutlineGeometry.getInstance().rowHeight : 0);
	}

	@Override
	public void doLayout() {
		super.doLayout();
		int contentOffset = nodePositioning.getDuplicateItemsHeight();
		int width = getWidth();
		int height = Math.max(0, getHeight() - contentOffset);
		blockPanel.setBounds(0, contentOffset, width, height);
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

        blockPanel.removeAll();
        blockCache.clear();
        blockLayout.resetCachedMaxWidth();
        resetBlockCache();
        navButtons.hideNavigationButtons();

        visibleNodes.setHoveredNode(hoveredNode);

        if (breadcrumbNodes != null) {
            breadcrumbPanel.update(breadcrumbNodes, true);
        } else {
            setBreadcrumbHeight(0);
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
        if ("useColoredOutlineItems".equals(propertyName)) {
        	refreshColoredOutlineItems();
        }
        else if ("outlineBreadcrumbMode".equals(propertyName)) {
        	setBreadcrumbMode(BreadcrumbMode.valueOf(newValue));
        }
    }

    private void refreshColoredOutlineItems() {
        for (BlockPanel panel : blockCache.values()) {
            panel.rebuildNodeButtons();
        }
        blockLayout.recomputeCachedMaxWidth();
        updatePreferredSize();

        breadcrumbPanel.updateNodeButtons();

        navButtons.hideNavigationButtons();
        reattachNavigationButtons();

        revalidate();
        repaint();
    }

    private void reattachNavigationButtons() {
    	TreeNode hoveredNode = visibleNodes.getHoveredNode();
        if (hoveredNode == null || hoveredNode.getChildren().isEmpty()) {
            return;
        }

        boolean inBreadcrumb = visibleNodes.isHoveredNodeContainedInBreadcrumb() && isNodeInBreadcrumbArea(hoveredNode);
        if (inBreadcrumb) {
            List<TreeNode> breadcrumbNodes = breadcrumbPanel.getCurrentBreadcrumbNodes();
            int rowIndex = breadcrumbNodes.indexOf(hoveredNode);
            if (rowIndex >= 0) {
                navButtons.attachToNode(hoveredNode, breadcrumbPanel, true, rowIndex, nodePositioning);
            }
            return;
        }

        navButtons.attachToNode(hoveredNode, blockPanel, false, -1, nodePositioning);
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

	BreadcrumbMode getBreadcrumbMode() {
		return breadcrumbMode;
	}

	void setBreadcrumbMode(BreadcrumbMode newMode) {
		BreadcrumbMode resolvedMode = newMode;
		if (breadcrumbMode == resolvedMode) {
			return;
		}
		breadcrumbMode = resolvedMode;
		nodePositioning.setDuplicateItemsHeight(calculateDuplicateItemsHeight());
		if (isSelectionDrivenBreadcrumbMode()) {
			updateBreadcrumbForSelection();
		}
		else {
			updateBreadcrumbForCurrentFirstVisibleNode();
		}
		updateVisibleBlocks();
	}

	boolean isSelectionDrivenBreadcrumbMode() {
		return breadcrumbMode == BreadcrumbMode.FOLLOW_SELECTED_ITEM;
	}


    void synchronizeSelectionButton(boolean requestFocusInWindow) {
    	selectionBridge.synchronizeOutlineSelection(requestFocusInWindow);
    	focusManager.focusSelectionButtonLater(requestFocusInWindow);
    }





    void setScrollPane(JScrollPane scroll) {
        this.viewport = new OutlineViewport(scroll, visibleNodes, nodePositioning);
        resetBlockCache();
    }

    void setBreadcrumbHeight(int height) {
        this.visibleNodes.setBreadcrumbHeight(height);
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
                && range.getBreadcrumbHeight() == lastBreadcrumbHeight
                && viewportWidth == lastViewportWidth
                && visibleCount == lastVisibleNodeCount;

        if (unchanged) {
            viewport.refreshViewport();
            updateFirstVisibleNodeId();
            return;
        }

        blockLayout.removeBlocksOutsideRange(blockPanel, range);
		blockLayout.updateVisibleBlocks(blockPanel, range, getWidth());


        lastFirstBlock = range.getFirstBlock();
        lastLastBlock = range.getLastBlock();
        lastBreadcrumbHeight = range.getBreadcrumbHeight();
        lastViewportWidth = viewportWidth;
        lastVisibleNodeCount = visibleCount;

        updatePreferredSize();
        refreshUI();
        updateFirstVisibleNodeId();

        TreeNode hoveredNode = visibleNodes.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.getChildren().isEmpty()) {
            boolean isInBreadcrumb = visibleNodes.isHoveredNodeContainedInBreadcrumb() && isNodeInBreadcrumbArea(hoveredNode);
            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, blockPanel, false, -1, nodePositioning);
            }
        }
        focusManager.restoreFocusIfNeeded(prevInOutline);
    }

    void updateVisibleBlocks(int startFromNodeIndex) {
        if (viewport == null) return;
        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);

        int breadcrumbHeight = visibleNodes.getBreadcrumbHeight();
        viewport.setViewPosition(startFromNodeIndex, breadcrumbHeight);

        OutlineVisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        blockLayout.removeBlocksOutsideRange(blockPanel, range);
		blockLayout.updateVisibleBlocks(blockPanel, range, getWidth());
        updatePreferredSize();
        refreshUI();

        range = viewport.calculateVisibleBlockRange(blockSize);
        lastFirstBlock = range.getFirstBlock();
        lastLastBlock = range.getLastBlock();
        lastBreadcrumbHeight = range.getBreadcrumbHeight();
        lastViewportWidth = viewport.getViewportWidth();
        lastVisibleNodeCount = visibleNodes.getVisibleNodeCount();
        updateFirstVisibleNodeId();

        TreeNode hoveredNode = visibleNodes.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.getChildren().isEmpty()) {
            boolean isInBreadcrumb = visibleNodes.isHoveredNodeContainedInBreadcrumb() && isNodeInBreadcrumbArea(hoveredNode);
            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, blockPanel, false, -1, nodePositioning);
            }
        }
        focusManager.restoreFocusIfNeeded(prevInOutline);
    }

    private void resetBlockCache() {
        lastFirstBlock = -1;
        lastLastBlock = -1;
        lastBreadcrumbHeight = -1;
        lastViewportWidth = -1;
        lastVisibleNodeCount = -1;
    }

    boolean isNodeFullyVisibleInViewport(TreeNode node) {
        if (viewport == null || node == null) return false;
        int index = visibleNodes.findNodeIndexInVisibleList(node);
        if (index < 0) return false;
        int first = viewport.calculateFirstVisibleNodeIndex();
        int currentBreadcrumbRows = getBreadcrumbRowsForIndex(first);
        int contentRows = getContentRowsForBreadcrumbRows(currentBreadcrumbRows);
        int last = Math.max(first, first + contentRows - 1);
        return index >= first && index <= last;
    }

    private void updatePreferredSize() {
        blockLayout.updateBlockPreferredSize(blockPanel);
        Dimension blockPreferredSize = blockPanel.getPreferredSize();
        int contentOffset = nodePositioning.getDuplicateItemsHeight();
        Dimension panelPreferredSize = new Dimension(blockPreferredSize.width,
                Math.max(blockPreferredSize.height, lastBreadcrumbHeight) + contentOffset);
        setPreferredSize(panelPreferredSize);
    }

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
		if(node != outlineSelection.getSelectedNode()) {
			if(visibleNodes.getHoveredNode() != node) {
				navButtons.hideNavigationButtons();
				visibleNodes.setHoveredNode(null);
			}
			selectionManager.select(this, outlineSelection, node, requestFocus);
		}
    }

    void toggleBreadcrumbNodeExpansion(TreeNode node, boolean requestFocus) {
        setSelectedNode(node, requestFocus);
        toggleExpandSelected();
    }

	void onSelectedNodeChanged() {
		if (isSelectionDrivenBreadcrumbMode()) {
			updateBreadcrumbForSelection();
		}
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
        blockPanel.removeAll();
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

        updatePreferredSize();
        refreshUI();
    }


    void rebuildFromNode(TreeNode anchorNode) {
        if (viewport == null) return;

        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);

        navButtons.hideNavigationButtons();

        visibleNodes.updateVisibleNodes();
		int firstFullyVisibleNodeIndex = viewport.calculateFirstVisibleNodeIndex();

		List<TreeNode> state = calculateBreadcrumbState(firstFullyVisibleNodeIndex);

        TreeNode preservedHovered = visibleNodes.getHoveredNode();
        blockPanel.removeAll();
        blockCache.clear();
        blockLayout.resetCachedMaxWidth();
        navButtons.hideNavigationButtons();
        final boolean wasHoveredNodeContainedInBreadcrumb = visibleNodes.isHoveredNodeContainedInBreadcrumb();
		visibleNodes.setHoveredNode(preservedHovered);

		if (state != null) {
			applyBreadcrumbState(state);
			updateVisibleBlocks(firstFullyVisibleNodeIndex);

            TreeNode hovered = visibleNodes.getHoveredNode();
			if (hovered != null && ! wasHoveredNodeContainedInBreadcrumb && visibleNodes.findNodeIndexInVisibleList(hovered) < 0) {
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

        blockPanel.removeAll();
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
        if (node != null && !node.getChildren().isEmpty()
        		&& (visibleNodes.isHoveredNodeContainedInBreadcrumb() || node != visibleNodes.getHoveredNode())) {

            if (! isNodeFullyVisibleInViewport(node)) {
                return;
            }

            visibleNodes.setHoveredNode(node, false);
            navButtons.attachToNode(node, blockPanel, false, -1, nodePositioning);
            repaint();
        }
    }
	void attachNavigationNode(TreeNode node, boolean isBreadCrumb, int rowIndex) {
		visibleNodes.setHoveredNode(node, isBreadCrumb);
		navButtons.attachToNode(node, isBreadCrumb ? breadcrumbPanel : blockPanel, isBreadCrumb, rowIndex, nodePositioning);
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
        if (isNodeInBreadcrumbArea(nextNode) || isNodeFullyVisibleInViewport(nextNode)) {
            setSelectedNode(nextNode, true);
            return;
        }
        int currentFirstVisibleIndex = viewport.calculateFirstVisibleNodeIndex();
		int currentBreadcrumbRowCount = getBreadcrumbRowsForIndex(currentFirstVisibleIndex);
		int nextBreadcrumbRowCount = getBreadcrumbRowsForIndex(Math.min(currentFirstVisibleIndex + 1, size - 1));
        int breadcrumbRowDelta = nextBreadcrumbRowCount - currentBreadcrumbRowCount; // изменение высоты крошек при продвижении верхнего на 1
        int minimalScrollRows = Math.max(0, 1 + breadcrumbRowDelta);

        int tentativeFirstIndex = Math.min(currentFirstVisibleIndex + minimalScrollRows, size - 1);
		int breadcrumbRowsAtTentative = getBreadcrumbRowsForIndex(tentativeFirstIndex);
        int contentRowCountAfterTentative = getContentRowsForBreadcrumbRows(breadcrumbRowsAtTentative);
        int maxFeasibleFirstIndex = Math.max(0, size - contentRowCountAfterTentative);
        int targetFirstIndex = Math.min(tentativeFirstIndex, maxFeasibleFirstIndex);
		List<TreeNode> plannedBreadcrumbState = calculateBreadcrumbState(targetFirstIndex);
		if (plannedBreadcrumbState != null) {
			applyBreadcrumbState(plannedBreadcrumbState);
			updateVisibleBlocks(targetFirstIndex);
		}

        setSelectedNode(nextNode, true);
    }

	private void updateBreadcrumbForSelection() {
		List<TreeNode> breadcrumbState = calculateBreadcrumbStateForSelection();
		applyBreadcrumbState(breadcrumbState);
	}

	private void updateBreadcrumbForCurrentFirstVisibleNode() {
		int firstVisibleIndex = 0;
		if (viewport != null) {
			firstVisibleIndex = viewport.calculateFirstVisibleNodeIndex();
		}
		List<TreeNode> breadcrumbState = calculateBreadcrumbStateForIndex(firstVisibleIndex);
		applyBreadcrumbState(breadcrumbState);
	}

	private void applyBreadcrumbState(List<TreeNode> breadcrumbState) {
		if (breadcrumbState != null) {
			breadcrumbPanel.update(breadcrumbState, false);
		}
		else {
			setBreadcrumbHeight(0);
			breadcrumbPanel.removeAll();
			breadcrumbPanel.revalidate();
			breadcrumbPanel.repaint();
		}
		reattachNavigationButtons();
	}

	private int getCurrentBreadcrumbRowCount() {
		int rowHeight = OutlineGeometry.getInstance().rowHeight;
		if (rowHeight <= 0) {
			return 0;
		}
		int height = visibleNodes.getBreadcrumbHeight();
		if (height <= 0) {
			return 0;
		}
		return Math.max(0, height / rowHeight);
	}

	private int getBreadcrumbRowsForIndex(int visibleIndex) {
		if (isSelectionDrivenBreadcrumbMode()) {
			return getCurrentBreadcrumbRowCount();
		}
		return getNodeLevelAtVisibleIndex(visibleIndex);
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
		if(selectionBridge != null)
			newBreadcrumbNodes.addAll(selectionBridge.collectNodesToSelection(outlineSelection.getSelectedNode()));
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
            if (currentIndex > currentFirstVisibleIndex) {
                TreeNode firstVisibleNode = visibleNodes.getNodeAtVisibleIndex(currentFirstVisibleIndex);
                if (firstVisibleNode != null) setSelectedNode(firstVisibleNode, true);
                updateVisibleBlocksAndBreadcrumb();
                return;
            }
            int tentativeFirstIndex = Math.max(0, currentFirstVisibleIndex - pageSize + 1); // overlap = 1
			int breadcrumbRowsAtTentative = getBreadcrumbRowsForIndex(tentativeFirstIndex);
            int contentRowCountAfterTentative = getContentRowsForBreadcrumbRows(breadcrumbRowsAtTentative);
            int maxFeasibleFirstIndex = Math.max(0, size - contentRowCountAfterTentative);
            int targetFirstIndex = Math.min(tentativeFirstIndex, maxFeasibleFirstIndex);
			List<TreeNode> plannedBreadcrumbState = calculateBreadcrumbState(targetFirstIndex);
			if (plannedBreadcrumbState != null) {
				applyBreadcrumbState(plannedBreadcrumbState);
				updateVisibleBlocks(targetFirstIndex);
			}
            TreeNode newFirstNode = visibleNodes.getNodeAtVisibleIndex(targetFirstIndex);
            if (newFirstNode != null) setSelectedNode(newFirstNode, true);
        }
    }

	private List<TreeNode> calculateBreadcrumbState(int targetFirstIndex) {
		List<TreeNode> plannedBreadcrumbState = isSelectionDrivenBreadcrumbMode()
			? calculateBreadcrumbStateForSelection()
			: calculateBreadcrumbStateForIndex(targetFirstIndex);
		return plannedBreadcrumbState;
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
			int breadcrumbRowsAtTentative = getBreadcrumbRowsForIndex(tentativeFirstIndex);
            int contentRowCountAfterTentative = getContentRowsForBreadcrumbRows(breadcrumbRowsAtTentative);
            int maxFeasibleFirstIndex = Math.max(0, size - contentRowCountAfterTentative);
            int targetFirstIndex = Math.min(tentativeFirstIndex, maxFeasibleFirstIndex);
			List<TreeNode> plannedBreadcrumbState = calculateBreadcrumbState(targetFirstIndex);
			if (plannedBreadcrumbState != null) {
				applyBreadcrumbState(plannedBreadcrumbState);
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
            int breadcrumbHeight = visibleNodes.getBreadcrumbHeight();
            int viewportHeight = viewport.getViewportHeight() - breadcrumbHeight;
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
        blockPanel.removeAll();
        blockCache.clear();
        navButtons.hideNavigationButtons();
        ensureSelectionVisibleTop();
    }

    void updateVisibleBlocksAndBreadcrumb() {
        Component prevFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        boolean prevInOutline = focusManager.isWithinOutline(prevFocus);
		int firstFullyVisibleNodeIndex = viewport.calculateFirstVisibleNodeIndex();
		List<TreeNode> state = calculateBreadcrumbState(firstFullyVisibleNodeIndex);
		if (state != null) {
			int oldBreadcrumbHeight = visibleNodes.getBreadcrumbHeight();
			applyBreadcrumbState(state);
            int newBreadcrumbHeight = visibleNodes.getBreadcrumbHeight();
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
		int currentBreadcrumbRows = getBreadcrumbRowsForIndex(first);
        int contentRows = getContentRowsForBreadcrumbRows(currentBreadcrumbRows);
        int last = Math.max(first, first + contentRows - 1);
        boolean haveButtons = blockPanel.getComponentCount() > 0 || !blockCache.isEmpty();
        if (!haveButtons && contentRows <= 1) {
            updateVisibleBlocks();
            return;
        }

        int visibleCount = visibleNodes.getVisibleNodeCount();
		if (haveButtons) {
			if (selected == null || selectedIndex < 0) return;
			if (!isSelectionDrivenBreadcrumbMode() && isNodeInBreadcrumbArea(selected)) return;
            if (selectedIndex >= first && selectedIndex <= last) return;
        }
        int targetFirst;
        if (selected == null || selectedIndex < 0) {
            targetFirst = first;
        }
		else if (!isSelectionDrivenBreadcrumbMode() && isNodeInBreadcrumbArea(selected)) {
			targetFirst = first;
		}
        else if (selectedIndex < first) {
            int desiredFirst = Math.max(0, selectedIndex);
			int br = getBreadcrumbRowsForIndex(desiredFirst);
            int cr = getContentRowsForBreadcrumbRows(br);
            int maxFeasibleFirst = Math.max(0, visibleCount - cr);
            targetFirst = Math.min(desiredFirst, maxFeasibleFirst);
        }
        else if (selectedIndex > last) {
            int desiredFirst = Math.max(0, selectedIndex - (contentRows - 1));
			int br = getBreadcrumbRowsForIndex(desiredFirst);
            int cr = getContentRowsForBreadcrumbRows(br);
            int maxFeasibleFirst = Math.max(0, visibleCount - cr);
            targetFirst = Math.min(desiredFirst, maxFeasibleFirst);
        }
        else {
            targetFirst = first;
        }
		List<TreeNode> planned = calculateBreadcrumbState(targetFirst);
		if (planned != null) {
			int oldBreadcrumbHeight = visibleNodes.getBreadcrumbHeight();
			applyBreadcrumbState(planned);
            int newBreadcrumbHeight = visibleNodes.getBreadcrumbHeight();
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

	void performInitialSetup() {
		if(isSelectionDrivenBreadcrumbMode())
			updateBreadcrumbForSelection();
		updateVisibleBlocks();
	}
}
