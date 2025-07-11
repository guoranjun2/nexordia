/*
 * Created on 8 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    final BreadcrumbPanel breadcrumbPanel;
    final OutlineGeometry geometry;

    final TreeNode root;
    final OutlineSelection selection;
    final int blockSize;
    List<FlatNode> visibleNodes = new ArrayList<>();
    final Map<Integer, BlockPanel> blockPanels = new HashMap<>();
    JScrollPane scrollPane;
    private int breadcrumbAreaHeight = 0;

    TreeNode hoveredNode;

    public ScrollableTreePanel(TreeNode root) {
		this(root, BLOCK_SIZE, null);
	}

    public ScrollableTreePanel(TreeNode root, BreadcrumbPanel breadcrumbPanel) {
		this(root, BLOCK_SIZE, breadcrumbPanel);
	}

	ScrollableTreePanel(TreeNode root, int blockSize, BreadcrumbPanel breadcrumbPanel) {
        super(null);
        this.root = root;
        this.blockSize = blockSize;
        this.breadcrumbPanel = breadcrumbPanel;
        this.selection = new OutlineSelection(root);
        this.hoveredNode = root;

        this.geometry = new OutlineGeometry(new javax.swing.JButton("▶"));
        this.navButtons = new NavigationButtons(geometry);
        this.selectionIcon = new SelectionCircleIcon(Color.BLUE, geometry.iconDiameter);

        root.applyExpansionLevel(1);

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
        this.scrollPane = scroll;
    }

    void setBreadcrumbAreaHeight(int height) {
        this.breadcrumbAreaHeight = height;
    }

    void updateVisibleNodeList() {
        visibleNodes = new ArrayList<>();
        buildVisibleList(root, 0);
    }

    void buildVisibleList(TreeNode node, int depth) {
        visibleNodes.add(new FlatNode(node, depth));
        if (node.isExpanded()) {
            for (TreeNode c : node.children) {
                buildVisibleList(c, depth + 1);
            }
        }
    }

        void updateVisibleBlocks() {
        if (scrollPane == null) return;

        updateVisibleNodeList();
        clearBlocks();
        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();

        if (hoveredNode != null && !hoveredNode.children.isEmpty()) {
            // Check if this node is in breadcrumb area - if so, don't move buttons here
            int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
            int nodeIndex = findNodeIndexInVisibleList(hoveredNode);
            boolean isInBreadcrumb = nodeIndex >= 0 && nodeIndex < breadcrumbNodeCount;

            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, breadcrumbAreaHeight, this);
            }
        }
    }

    void updateVisibleBlocks(int startFromNodeIndex) {
        if (scrollPane == null) return;

        updateVisibleNodeList();
        clearBlocks();

        // Position viewport so that startFromNodeIndex appears at the top of content area (below breadcrumb)
        // We need to account for the breadcrumb area height
        int targetY = (startFromNodeIndex * geometry.rowHeight) - breadcrumbAreaHeight;
        targetY = Math.max(0, targetY); // Ensure we don't go negative
        System.out.println("updateVisibleBlocks: startFromNodeIndex=" + startFromNodeIndex +
                          ", breadcrumbAreaHeight=" + breadcrumbAreaHeight +
                          ", targetY=" + targetY);
        scrollPane.getViewport().setViewPosition(new java.awt.Point(0, targetY));

        createVisibleBlocks();
        updatePreferredFromActualBlocks();
        refreshUI();

        if (hoveredNode != null && !hoveredNode.children.isEmpty()) {
            // Check if this node is in breadcrumb area - if so, don't move buttons here
            int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
            int nodeIndex = findNodeIndexInVisibleList(hoveredNode);
            boolean isInBreadcrumb = nodeIndex >= 0 && nodeIndex < breadcrumbNodeCount;

            if (!isInBreadcrumb) {
                navButtons.attachToNode(hoveredNode, this, false, -1, breadcrumbAreaHeight, this);
            }
        }
    }

    private void clearBlocks() {
        for (BlockPanel panel : blockPanels.values()) {
            remove(panel);
        }
        blockPanels.clear();

        Component[] components = getComponents();
        for (Component comp : components) {
            if (comp instanceof JButton && !isNavigationButton((JButton) comp)) {
                remove(comp);
            }
        }
        revalidate();
    }

    private boolean isNavigationButton(JButton button) {
        return button == navButtons.expandBtn || button == navButtons.collapseBtn ||
               button == navButtons.expandMoreBtn || button == navButtons.reduceBtn;
    }

    private void createVisibleBlocks() {
        Rectangle view = scrollPane.getViewport().getViewRect();
        int blockHeight = blockSize * geometry.rowHeight;
        int totalBlocks = (visibleNodes.size() + blockSize - 1) / blockSize;

        int adjustedViewY = Math.max(0, view.y - breadcrumbAreaHeight);
        int adjustedViewHeight = view.height;

        int firstBlock = Math.max(0, adjustedViewY / blockHeight);
        int lastBlock = Math.min(totalBlocks - 1, (adjustedViewY + adjustedViewHeight) / blockHeight);

        for (int b = firstBlock; b <= lastBlock; b++) {
            createBlock(b, breadcrumbAreaHeight);
        }
    }

    private void createBlock(int blockIndex, int yOffset) {
        int start = blockIndex * blockSize;
        int end = Math.min(start + blockSize, visibleNodes.size());
        int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;

        // Skip blocks that would be entirely composed of breadcrumb nodes
        if (end <= breadcrumbNodeCount) {
            return;
        }

        BlockPanel bp = new BlockPanel(visibleNodes.subList(start, end), start, geometry.rowHeight, geometry.indent, this, breadcrumbNodeCount, selection);

        // Calculate position and height only for visible (non-breadcrumb) nodes
        int visibleStart = Math.max(start, breadcrumbNodeCount);
        int visibleNodesInBlock = end - visibleStart;
        int blockY = yOffset + (visibleStart - breadcrumbNodeCount) * geometry.rowHeight;

        bp.setBounds(0, blockY, getPreferredSize().width, visibleNodesInBlock * geometry.rowHeight);
        add(bp);
        blockPanels.put(blockIndex, bp);
    }

    private void updatePreferredFromActualBlocks() {
        int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
        int contentNodesCount = Math.max(0, visibleNodes.size() - breadcrumbNodeCount);
        int height = breadcrumbAreaHeight + contentNodesCount * geometry.rowHeight;
        int maxWidth = calculateActualRequiredWidth();
        setPreferredSize(new Dimension(maxWidth, Math.max(height, 600)));

        for (BlockPanel panel : blockPanels.values()) {
            Dimension currentSize = panel.getSize();
            panel.setSize(maxWidth, currentSize.height);
        }
    }

    private int calculateActualRequiredWidth() {
        int maxWidth = 400;
        for (BlockPanel panel : blockPanels.values()) {
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
        scrollPane.getViewport().revalidate();
        scrollPane.repaint();
        repaint();
    }

    public void addChildNode(TreeNode parent, String childName, String childId) {
        TreeNode newChild = new TreeNode(childName, childId);
        parent.addChild(newChild);
        updateVisibleBlocks();
    }

    public void removeNode(TreeNode nodeToRemove) {
        if (nodeToRemove.parent != null) {
            nodeToRemove.parent.removeChild(nodeToRemove);
            updateVisibleBlocks();
        }
    }

    public void setSelectedNodeId(String nodeId) {
        if (nodeId != null && selection.findNodeById(nodeId) != null) {
            selection.selectNode(nodeId);
            TreeNode preservedHoveredNode = hoveredNode;
            removeAll();
            blockPanels.clear();
            navButtons.hideNavigationButtons();
            hoveredNode = preservedHoveredNode;
            updateVisibleBlocks();
            SwingUtilities.invokeLater(this::scrollToSelectedNode);
        }
    }



    private void scrollToSelectedNode() {
        if (scrollPane == null || selection.getSelectedNodeId() == null) {
            return;
        }

        int selectedIndex = findSelectedNodeIndex();
        if (selectedIndex >= 0) {
            int y = breadcrumbAreaHeight + selectedIndex * geometry.rowHeight;
            Rectangle targetRect = new Rectangle(0, y, getWidth(), geometry.rowHeight);
            scrollRectToVisible(targetRect);
        }
    }

    private int findSelectedNodeIndex() {
        for (int i = 0; i < visibleNodes.size(); i++) {
            if (visibleNodes.get(i).node.id.equals(selection.getSelectedNodeId())) {
                return i;
            }
        }
        return -1;
    }

    public void selectNodeByTitle(String title) {
        TreeNode found = findNodeByTitle(root, title);
        if (found != null) {
            setSelectedNodeId(found.id);
        }
    }

    private TreeNode findNodeByTitle(TreeNode node, String title) {
        if (node.title.equals(title)) {
            return node;
        }
        for (TreeNode child : node.children) {
            TreeNode found = findNodeByTitle(child, title);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    void selectNodeById(String nodeId) {
        setSelectedNodeId(nodeId);
    }

    public String getSelectedNodeId() {
        return selection.getSelectedNodeId();
    }

    public TreeNode getSelectedNode() {
        return selection.getSelectedNode();
    }

    void onContentButtonHovered(TreeNode node) {
        if (node != null && !node.children.isEmpty() && node != hoveredNode) {
            // Check if this node is in breadcrumb area - if so, don't handle hover here
            int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
            int nodeIndex = findNodeIndexInVisibleList(node);
            if (nodeIndex >= 0 && nodeIndex < breadcrumbNodeCount) {
                // Node is in breadcrumb, don't handle hover in main content
                return;
            }

            hoveredNode = node;
            navButtons.attachToNode(node, this, false, -1, breadcrumbAreaHeight, this);
            repaint();
        }
    }

    void refreshWithBreadcrumbs() {
        TreeNode preservedHoveredNode = hoveredNode;
        SwingUtilities.invokeLater(() -> {
        	updateVisibleBlocksAndBreadcrumb();
        	refreshPanel();

            // Immediately recreate navigation buttons for the hovered node if it has children
                        if (preservedHoveredNode != null && !preservedHoveredNode.children.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    hoveredNode = preservedHoveredNode;
                    // Check if node is in breadcrumb area
                    int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
                    int nodeIndex = findNodeIndexInVisibleList(preservedHoveredNode);
                    boolean isInBreadcrumb = nodeIndex >= 0 && nodeIndex < breadcrumbNodeCount;

                    if (isInBreadcrumb) {
                       breadcrumbPanel.updateNavigationButtons();
                    } else {
                        // Node is in main content
                        navButtons.attachToNode(preservedHoveredNode, this, false, -1, breadcrumbAreaHeight, this);
                    }
                });
            }
        });
    }



    private void refreshPanel() {
        SwingUtilities.invokeLater(() -> {
            TreeNode preservedHoveredNode = hoveredNode;
            navButtons.hideNavigationButtons();
            removeAll();
            blockPanels.clear();
            navButtons.hideNavigationButtons();
            hoveredNode = preservedHoveredNode;
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
        int currentIndex = findSelectedNodeIndex();
        if (currentIndex > 0) {
            TreeNode newSelected = visibleNodes.get(currentIndex - 1).node;
            setSelectedNodeId(newSelected.id);
        }
    }

    private void navigateDown() {
        int currentIndex = findSelectedNodeIndex();
        if (currentIndex >= 0 && currentIndex < visibleNodes.size() - 1) {
            TreeNode newSelected = visibleNodes.get(currentIndex + 1).node;
            setSelectedNodeId(newSelected.id);
        }
    }

    private void navigatePageUp() {
        int currentIndex = findSelectedNodeIndex();
        int pageSize = getPageSize();
        int newIndex = Math.max(0, currentIndex - pageSize);
        if (newIndex != currentIndex) {
            TreeNode newSelected = visibleNodes.get(newIndex).node;
            setSelectedNodeId(newSelected.id);
        }
    }

    private void navigatePageDown() {
        int currentIndex = findSelectedNodeIndex();
        int pageSize = getPageSize();
        int newIndex = Math.min(visibleNodes.size() - 1, currentIndex + pageSize);
        if (newIndex != currentIndex) {
            TreeNode newSelected = visibleNodes.get(newIndex).node;
            setSelectedNodeId(newSelected.id);
        }
    }

    private int getPageSize() {
        if (scrollPane != null) {
            int viewportHeight = scrollPane.getViewport().getHeight();
            return Math.max(1, viewportHeight / geometry.rowHeight);
        }
        return 10;
    }

    int findNodeIndexInVisibleList(TreeNode node) {
        for (int i = 0; i < visibleNodes.size(); i++) {
            if (visibleNodes.get(i).node == node) {
                return i;
            }
        }
        return -1;
    }

    private List<TreeNode> getCurrentBreadcrumbNodes() {
    	return breadcrumbPanel.getCurrentBreadcrumbNodes();
    }

    void updateVisibleBlocksAndBreadcrumb() {
        BreadcrumbState state = calculateBreadcrumbState();
        if (state != null) {
            breadcrumbPanel.update(state);
            if (state.levelReductionFirstVisibleNodeIndex >= 0) {
                updateVisibleBlocks(state.levelReductionFirstVisibleNodeIndex);
            }
            else
                updateVisibleBlocks();
        }
        else
            updateVisibleBlocks();
    }

    private BreadcrumbState calculateBreadcrumbState() {
        if (visibleNodes.isEmpty()) {
            return null;
        }

        java.awt.Rectangle viewRect = scrollPane.getViewport().getViewRect();

        int currentBreadcrumbHeight = breadcrumbAreaHeight;

        int effectiveViewportY = viewRect.y + currentBreadcrumbHeight;
        int firstFullyVisibleNodeIndex = Math.max(0, (effectiveViewportY + geometry.rowHeight/2 - 1) / geometry.rowHeight);

        if (firstFullyVisibleNodeIndex >= visibleNodes.size()) {
            return null;
        }

        TreeNode firstFullyVisibleNode = visibleNodes.get(firstFullyVisibleNodeIndex).node;

        List<TreeNode> currentBreadcrumbNodes = getCurrentBreadcrumbNodes();
        if (firstFullyVisibleNode == root) {
        	if(currentBreadcrumbHeight == 0)
        		return null;
        	else
        		return new BreadcrumbState(Collections.emptyList(), 0, 0);
        }

        TreeNode lastCurrentBreadcrumbNode = currentBreadcrumbNodes.isEmpty() ? null : currentBreadcrumbNodes.get(currentBreadcrumbNodes.size() - 1);

        if (firstFullyVisibleNode.parent == lastCurrentBreadcrumbNode) {
            return null;
        }

        List<TreeNode> newBreadcrumbNodes = collectBreadCrumbNodes(firstFullyVisibleNode);
        int newBreadcrumbHeight = newBreadcrumbNodes.size() * geometry.rowHeight;

        return new BreadcrumbState(newBreadcrumbNodes, newBreadcrumbHeight, firstFullyVisibleNodeIndex);
    }

	private List<TreeNode> collectBreadCrumbNodes(TreeNode firstFullyVisibleNode) {
		List<TreeNode> breadcrumbNodes = new ArrayList<>();
		TreeNode current = firstFullyVisibleNode.parent;
		while (current != null) {
		    breadcrumbNodes.add(0, current);
		    current = current.parent;
		}
		return breadcrumbNodes;
	}

    int calculateNodeDepth(TreeNode node) {
        int depth = 0;
        TreeNode current = node;
        while (current != root) {
            current = current.parent;
            depth++;
        }
        return depth;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

	public void attachNavigationNode(TreeNode node,
	        boolean isBreadCrumb, int rowIndex, int currentBreadcrumbHeight) {
		hoveredNode = isBreadCrumb ? null : node;
		navButtons.attachToNode(node, breadcrumbPanel, isBreadCrumb, rowIndex, currentBreadcrumbHeight, this);
	}
}