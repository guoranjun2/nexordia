/*
 * Created on 8 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

class ScrollableTreePanel extends JPanel {
    private static final int BLOCK_SIZE = 50;
	private static final long serialVersionUID = -5362893875920096494L;

    final NavigationButtons navButtons;
    final SelectionCircleIcon selectionIcon;
    private final BreadcrumbPanel breadcrumbPanel;
    final OutlineGeometry geometry;
    private final ExpansionControls expansionControls;
    private NodePositioning nodePositioning;
    final BreadcrumbPath breadcrumbPath;
    OutlineViewport viewport;

    private TreeNode root;
    private OutlineSelection selection;
    private final int blockSize;
    private VisibleOutlineState visibleState;

    // Cached state to avoid unnecessary block rebuilds
    private int lastFirstBlock = -1;
    private int lastLastBlock = -1;
    private int lastBreadcrumbAreaHeight = -1;
    private int lastViewportWidth = -1;
    private int lastVisibleNodeCount = -1;

    public ScrollableTreePanel(TreeNode root, BreadcrumbPanel breadcrumbPanel) {
		this(root, BLOCK_SIZE, breadcrumbPanel);
	}

    private ScrollableTreePanel(TreeNode root, int blockSize, BreadcrumbPanel breadcrumbPanel) {
        super(null);
        this.root = root;
        this.blockSize = blockSize;
        this.breadcrumbPanel = breadcrumbPanel;
        root.applyExpansionLevel(1);
        this.selection = new OutlineSelection(root);
        this.visibleState = new VisibleOutlineState(root);

        this.geometry = new OutlineGeometry(new javax.swing.JButton("▶"));
        this.expansionControls = new ExpansionControls(this);
        this.nodePositioning = new NodePositioning(root, geometry, visibleState);
        this.breadcrumbPath = new BreadcrumbPath(root, geometry, visibleState, null);
        this.navButtons = new NavigationButtons(geometry, expansionControls);
        this.selectionIcon = new SelectionCircleIcon(Color.BLUE, geometry.iconDiameter);


        setBackground(Color.WHITE);
        setFocusable(true);
        setupKeyBindings();

        add(navButtons.expandBtn);
        add(navButtons.collapseBtn);
        add(navButtons.expandMoreBtn);
        add(navButtons.reduceBtn);

        navButtons.hideNavigationButtons();
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

        // Fast-path: If nothing relevant changed, skip expensive rebuild.
        OutlineViewport.VisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        int viewportWidth = viewport.getViewportWidth();
        int visibleCount = visibleState.getVisibleNodeCount();
        boolean haveBlocks = !visibleState.getBlockPanels().isEmpty();
        boolean unchanged = haveBlocks
                && range.firstBlock == lastFirstBlock
                && range.lastBlock == lastLastBlock
                && range.breadcrumbAreaHeight == lastBreadcrumbAreaHeight
                && viewportWidth == lastViewportWidth
                && visibleCount == lastVisibleNodeCount;

        if (unchanged) {
            viewport.refreshViewport();
            return;
        }

        clearBlocks();
        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();

        // Update cache after successful rebuild
        lastFirstBlock = range.firstBlock;
        lastLastBlock = range.lastBlock;
        lastBreadcrumbAreaHeight = range.breadcrumbAreaHeight;
        lastViewportWidth = viewportWidth;
        lastVisibleNodeCount = visibleCount;

        TreeNode hoveredNode = visibleState.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.children.isEmpty()) {
            // Check if this node is in breadcrumb area - if so, don't move buttons here
            boolean isInBreadcrumb = visibleState.isNodeInBreadcrumbArea(hoveredNode, geometry.rowHeight);

            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, visibleState.getBreadcrumbAreaHeight(), nodePositioning);
            }
        }
    }

    private void updateVisibleBlocks(int startFromNodeIndex) {
        if (viewport == null) return;

        clearBlocks();

        // Position viewport so that startFromNodeIndex appears at the top of content area (below breadcrumb)
        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        viewport.setViewPosition(startFromNodeIndex, breadcrumbAreaHeight);
        System.out.println("updateVisibleBlocks: startFromNodeIndex=" + startFromNodeIndex +
                          ", breadcrumbAreaHeight=" + breadcrumbAreaHeight);

        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();

        // Update cache after rebuild triggered by explicit repositioning
        OutlineViewport.VisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        lastFirstBlock = range.firstBlock;
        lastLastBlock = range.lastBlock;
        lastBreadcrumbAreaHeight = range.breadcrumbAreaHeight;
        lastViewportWidth = viewport.getViewportWidth();
        lastVisibleNodeCount = visibleState.getVisibleNodeCount();

        TreeNode hoveredNode = visibleState.getHoveredNode();
        if (hoveredNode != null && !hoveredNode.children.isEmpty()) {
            // Check if this node is in breadcrumb area - if so, don't move buttons here
            boolean isInBreadcrumb = visibleState.isNodeInBreadcrumbArea(hoveredNode, geometry.rowHeight);

            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, breadcrumbAreaHeight, nodePositioning);
            }
        }
    }

    private void clearBlocks() {
        // Remove tracked block panels
        for (BlockPanel panel : visibleState.getBlockPanels().values()) {
            remove(panel);
        }
        visibleState.clearBlockPanels();

        // Defensive cleanup: remove any BlockPanel still attached but not tracked
        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof BlockPanel) {
                remove(comp);
            }
        }

        // Remove any stray buttons (not navigation buttons)
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

    private boolean isNavigationButton(JButton button) {
        return button == navButtons.expandBtn || button == navButtons.collapseBtn ||
               button == navButtons.expandMoreBtn || button == navButtons.reduceBtn;
    }

    private void createVisibleBlocks() {
        OutlineViewport.VisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);

        for (int b = range.firstBlock; b <= range.lastBlock; b++) {
            if (!visibleState.hasBlockPanel(b))
                createBlock(b, range.breadcrumbAreaHeight);
        }
    }

    private void createBlock(int blockIndex, int yOffset) {
        int start = blockIndex * blockSize;
        List<FlatNode> visibleNodes = visibleState.getVisibleNodes();
        int end = Math.min(start + blockSize, visibleNodes.size());
        int breadcrumbNodeCount = visibleState.getBreadcrumbAreaHeight() / geometry.rowHeight;

        // Skip blocks that would be entirely composed of breadcrumb nodes
        if (end <= breadcrumbNodeCount) {
            return;
        }

        BlockPanel bp = new BlockPanel(visibleNodes.subList(start, end), start, geometry.rowHeight, geometry.indent, this, breadcrumbNodeCount, selection);

        Rectangle bounds = nodePositioning.calculateBlockBounds(blockIndex, blockSize, yOffset, getPreferredSize().width);
        bp.setBounds(bounds);
        add(bp);
        visibleState.addBlockPanel(blockIndex, bp);
    }

    private void updatePreferredFromActualBlocks() {
        int breadcrumbAreaHeight = visibleState.getBreadcrumbAreaHeight();
        int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
        int contentNodesCount = Math.max(0, visibleState.getVisibleNodeCount() - breadcrumbNodeCount);
        int height = breadcrumbAreaHeight + contentNodesCount * geometry.rowHeight;
        int maxWidth = calculateActualRequiredWidth();
        setPreferredSize(new Dimension(maxWidth, height));

        for (BlockPanel panel : visibleState.getBlockPanels().values()) {
            Dimension currentSize = panel.getSize();
            panel.setSize(maxWidth, currentSize.height);
        }
    }

    private int calculateActualRequiredWidth() {
        int maxWidth = 400;
        for (BlockPanel panel : visibleState.getBlockPanels().values()) {
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

    public void setSelectedNodeId(String nodeId) {
        if (nodeId != null && selection.findNodeById(nodeId) != null) {
            selection.selectNode(nodeId);
            TreeNode preservedHoveredNode = visibleState.getHoveredNode();
            removeAll();
            visibleState.clearBlockPanels();
            navButtons.hideNavigationButtons();
            visibleState.setHoveredNode(preservedHoveredNode);
            updateVisibleBlocks();
            SwingUtilities.invokeLater(this::scrollToSelectedNode);
        }
    }


    public OutlineSelection getSelection() {
        return selection;
    }

    public VisibleOutlineState getVisibleState() {
        return visibleState;
    }

    public NodePositioning getNodePositioning() {
        return nodePositioning;
    }

    public TreeNode getRoot() {
        return root;
    }



    private void scrollToSelectedNode() {
        TreeNode selectedNode = selection.getSelectedNode();
        if (selectedNode != null && viewport != null) {
            viewport.scrollToNode(selectedNode);
        }
    }



    public void selectNodeById(String nodeId) {
        selection.selectNode(nodeId);
        repaint();
    }

    public String getSelectedNodeId() {
        return selection.getSelectedNodeId();
    }

    public TreeNode getSelectedNode() {
        return selection.getSelectedNode();
    }

    // Incremental update for title change without full rebuild
    void updateNodeTitle(TreeNode node) {
        // Update in breadcrumb if present
        for (Component comp : breadcrumbPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                Object n = btn.getClientProperty("treeNode");
                if (n == node) {
                    btn.setText(node.title);
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

        // Update in visible content blocks if present
        for (int blockIndex : visibleState.getBlockPanelIndices()) {
            BlockPanel panel = visibleState.getBlockPanel(blockIndex);
            if (panel == null) continue;
            for (Component comp : panel.getComponents()) {
                if (comp instanceof JButton) {
                    JButton btn = (JButton) comp;
                    Object n = btn.getClientProperty("treeNode");
                    if (n == node) {
                        btn.setText(node.title);
                        int depth = calculateNodeDepth(node);
                        if (depth >= 0) {
                            int x = geometry.calculateTextButtonX(depth);
                            btn.setBounds(x, btn.getY(), btn.getPreferredSize().width, geometry.rowHeight);
                        }
                        panel.revalidate();
                        panel.repaint();
                        // Node appears only once
                        break;
                    }
                }
            }
        }

        updatePreferredFromActualBlocks();
        refreshUI();
    }

    // Incremental rebuild starting from an anchor node (e.g., parent on insert/delete)
    void rebuildFromNode(TreeNode anchorNode) {
        if (viewport == null) return;

        // Avoid stale buttons attached to nodes being deleted/moved
        navButtons.hideNavigationButtons();

        visibleState.updateVisibleNodes();

        // If breadcrumb needs to change, apply the stable path that repositions content
        BreadcrumbState state = calculateBreadcrumbState();
        if (state != null) {
            breadcrumbPanel.update(state);
            updateVisibleBlocks(state.firstVisibleNodeIndex);
            // Validate hovered node after rebuild
            TreeNode hovered = visibleState.getHoveredNode();
            if (hovered != null && visibleState.findNodeIndexInVisibleList(hovered) < 0) {
                visibleState.setHoveredNode(null);
            }
            return;
        }

        int anchorIndex = visibleState.findNodeIndexInVisibleList(anchorNode);
        if (anchorIndex < 0) {
            // Not visible (collapsed). Let normal path decide.
            updateVisibleBlocksAndBreadcrumb();
            return;
        }

        OutlineViewport.VisibleBlockRange range = viewport.calculateVisibleBlockRange(blockSize);
        int startBlock = Math.max(0, anchorIndex / blockSize);

        removeBlocksFromBlockIndex(startBlock);
        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();

        // Update cache
        lastFirstBlock = range.firstBlock;
        lastLastBlock = range.lastBlock;
        lastBreadcrumbAreaHeight = range.breadcrumbAreaHeight;
        lastViewportWidth = viewport.getViewportWidth();
        lastVisibleNodeCount = visibleState.getVisibleNodeCount();

        // Validate hovered node after partial rebuild
        TreeNode hovered = visibleState.getHoveredNode();
        if (hovered != null && visibleState.findNodeIndexInVisibleList(hovered) < 0) {
            visibleState.setHoveredNode(null);
        }
    }

    private void removeBlocksFromBlockIndex(int startBlock) {
        java.util.List<Integer> indices = new java.util.ArrayList<>(visibleState.getBlockPanelIndices());
        for (int idx : indices) {
            if (idx >= startBlock) {
                BlockPanel p = visibleState.getBlockPanel(idx);
                if (p != null) remove(p);
                visibleState.removeBlockPanel(idx);
            }
        }
    }

    void onContentButtonHovered(TreeNode node) {
        TreeNode hoveredNode = visibleState.getHoveredNode();
        if (node != null && !node.children.isEmpty() && node != hoveredNode) {
            // Check if this node is in breadcrumb area - if so, don't handle hover here
            if (visibleState.isNodeInBreadcrumbArea(node, geometry.rowHeight)) {
                // Node is in breadcrumb, don't handle hover in main content
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
            // Hide current nav buttons to avoid transient misplacement during rebuild
            navButtons.hideNavigationButtons();

            // Single-pass rebuild: updates breadcrumb and blocks with correct positioning
            updateVisibleBlocksAndBreadcrumb();

            // Recreate navigation buttons for the hovered node if it has children
            if (preservedHoveredNode != null && !preservedHoveredNode.children.isEmpty()) {
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



    private void refreshPanel() {
        SwingUtilities.invokeLater(() -> {
            TreeNode preservedHoveredNode = visibleState.getHoveredNode();
            navButtons.hideNavigationButtons();
            clearBlocks();
            navButtons.hideNavigationButtons();
            visibleState.setHoveredNode(preservedHoveredNode);
            updateVisibleBlocks();
        });
    }

    private void setupKeyBindings() {
        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = getActionMap();

        inputMap.put(KeyStroke.getKeyStroke("UP"), "navigateUp");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "navigateDown");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_UP"), "navigatePageUp");
        inputMap.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "navigatePageDown");

        actionMap.put("navigateUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateUp();
            }
        });

        actionMap.put("navigateDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigateDown();
            }
        });

        actionMap.put("navigatePageUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigatePageUp();
            }
        });

        actionMap.put("navigatePageDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                navigatePageDown();
            }
        });
    }

    private void navigateUp() {
        TreeNode currentSelected = selection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            if (currentIndex > 0) {
                List<FlatNode> visibleNodes = visibleState.getVisibleNodes();
                TreeNode newSelected = visibleNodes.get(currentIndex - 1).node;
                setSelectedNodeId(newSelected.id);
            }
        }
    }

    private void navigateDown() {
        TreeNode currentSelected = selection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            List<FlatNode> visibleNodes = visibleState.getVisibleNodes();
            if (currentIndex >= 0 && currentIndex < visibleNodes.size() - 1) {
                TreeNode newSelected = visibleNodes.get(currentIndex + 1).node;
                setSelectedNodeId(newSelected.id);
            }
        }
    }

    private void navigatePageUp() {
        TreeNode currentSelected = selection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getPageSize();
            int newIndex = Math.max(0, currentIndex - pageSize);
            if (newIndex != currentIndex) {
                List<FlatNode> visibleNodes = visibleState.getVisibleNodes();
                TreeNode newSelected = visibleNodes.get(newIndex).node;
                setSelectedNodeId(newSelected.id);
            }
        }
    }

    private void navigatePageDown() {
        TreeNode currentSelected = selection.getSelectedNode();
        if (currentSelected != null) {
            int currentIndex = visibleState.findNodeIndexInVisibleList(currentSelected);
            int pageSize = getPageSize();
            List<FlatNode> visibleNodes = visibleState.getVisibleNodes();
            int newIndex = Math.min(visibleNodes.size() - 1, currentIndex + pageSize);
            if (newIndex != currentIndex) {
                TreeNode newSelected = visibleNodes.get(newIndex).node;
                setSelectedNodeId(newSelected.id);
            }
        }
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
		BreadcrumbState state = calculateBreadcrumbState();
	    if (state != null) {
	        breadcrumbPanel.update(state);
	        updateVisibleBlocks(state.firstVisibleNodeIndex);
	    }
	    else {
	        // Breadcrumb unchanged; still refresh visible blocks in place
	        updateVisibleBlocks();
	    }
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

	public void attachNavigationNode(TreeNode node,
	        boolean isBreadCrumb, int rowIndex, int currentBreadcrumbHeight) {
		visibleState.setHoveredNode(node);
		navButtons.attachToNode(node, breadcrumbPanel, isBreadCrumb, rowIndex, currentBreadcrumbHeight, nodePositioning);
	}
}
