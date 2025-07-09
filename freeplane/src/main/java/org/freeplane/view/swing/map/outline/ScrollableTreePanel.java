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

    final TreeNode root;
    final TreeSelection selection;
    final int blockSize;
    List<FlatNode> visibleNodes = new ArrayList<>();
    final Map<Integer, BlockPanel> blockPanels = new HashMap<>();
    JScrollPane scrollPane;
    private int breadcrumbAreaHeight = 0;

    TreeNode hoveredNode;

    public ScrollableTreePanel(TreeNode root) {
		this(root, BLOCK_SIZE);
	}

	ScrollableTreePanel(TreeNode root,int blockSize) {
        super(null);
        this.root = root;
        this.blockSize = blockSize;
        this.selection = new TreeSelection(root);
        this.hoveredNode = root;

        this.navButtons = new NavigationButtons();
        this.selectionIcon = new SelectionCircleIcon(Color.BLUE, navButtons.iconDiameter);

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
            int breadcrumbNodeCount = breadcrumbAreaHeight / navButtons.rowHeight;
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
        int targetY = (startFromNodeIndex * navButtons.rowHeight) - breadcrumbAreaHeight;
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
            int breadcrumbNodeCount = breadcrumbAreaHeight / navButtons.rowHeight;
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
        int blockHeight = blockSize * navButtons.rowHeight;
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
        int breadcrumbNodeCount = breadcrumbAreaHeight / navButtons.rowHeight;

        // Skip blocks that would be entirely composed of breadcrumb nodes
        if (end <= breadcrumbNodeCount) {
            return;
        }

        BlockPanel bp = new BlockPanel(visibleNodes.subList(start, end), start, navButtons.rowHeight, navButtons.indent, this, breadcrumbNodeCount, selection);

        // Calculate position and height only for visible (non-breadcrumb) nodes
        int visibleStart = Math.max(start, breadcrumbNodeCount);
        int visibleNodesInBlock = end - visibleStart;
        int blockY = yOffset + (visibleStart - breadcrumbNodeCount) * navButtons.rowHeight;

        bp.setBounds(0, blockY, getPreferredSize().width, visibleNodesInBlock * navButtons.rowHeight);
        add(bp);
        blockPanels.put(blockIndex, bp);
    }

    private void updatePreferredFromActualBlocks() {
        int breadcrumbNodeCount = breadcrumbAreaHeight / navButtons.rowHeight;
        int contentNodesCount = Math.max(0, visibleNodes.size() - breadcrumbNodeCount);
        int height = breadcrumbAreaHeight + contentNodesCount * navButtons.rowHeight;
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
            int y = breadcrumbAreaHeight + selectedIndex * navButtons.rowHeight;
            Rectangle targetRect = new Rectangle(0, y, getWidth(), navButtons.rowHeight);
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
            int breadcrumbNodeCount = breadcrumbAreaHeight / navButtons.rowHeight;
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



    private FlatNode findFlatNode(TreeNode node) {
        for (FlatNode flat : visibleNodes) {
            if (flat.node == node) {
                return flat;
            }
        }
        return null;
    }



    private int calculateBaseXForDepth(int depth) {
        if (depth == 0) {
            return 0;
        } else {
            return (depth * navButtons.indent) - navButtons.indent;
        }
    }



    void refreshWithBreadcrumbs() {
        TreeNode preservedHoveredNode = hoveredNode;
        SwingUtilities.invokeLater(() -> {
            // Notify TreeViewPane to update breadcrumbs first
            Container parent = getParent();
            while (parent != null && !(parent instanceof OutlinePane)) {
                parent = parent.getParent();
            }
            if (parent instanceof OutlinePane) {
                BreadcrumbState state = calculateBreadcrumbState();
                ((OutlinePane) parent).breadcrumbPanel.update(state);
            }
            refreshPanel();

            // Immediately recreate navigation buttons for the hovered node if it has children
            if (preservedHoveredNode != null && !preservedHoveredNode.children.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    hoveredNode = preservedHoveredNode;
                    // Check if node is in breadcrumb area
                    int breadcrumbNodeCount = breadcrumbAreaHeight / navButtons.rowHeight;
                    int nodeIndex = findNodeIndexInVisibleList(preservedHoveredNode);
                    boolean isInBreadcrumb = nodeIndex >= 0 && nodeIndex < breadcrumbNodeCount;

                    if (isInBreadcrumb) {
                        // Find the row index in breadcrumb
                        Container outlinePane = getParent();
                        while (outlinePane != null && !(outlinePane instanceof OutlinePane)) {
                            outlinePane = outlinePane.getParent();
                        }
                        if (outlinePane instanceof OutlinePane) {
                            // Let OutlinePane handle breadcrumb button positioning
                            BreadcrumbState state = calculateBreadcrumbState();
                            ((OutlinePane) outlinePane).breadcrumbPanel.update(state);
                        }
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
            return Math.max(1, viewportHeight / navButtons.rowHeight);
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

    BreadcrumbState calculateBreadcrumbState() {
        if (visibleNodes.isEmpty()) {
            return new BreadcrumbState(java.util.Collections.emptyList(), 0, false, -1);
        }

        java.awt.Rectangle viewRect = scrollPane.getViewport().getViewRect();
        System.out.println("=== calculateBreadcrumbState START ===");
        System.out.println("ViewRect: " + viewRect);

        int workingBreadcrumbHeight = 0;
        TreeNode previousFirstVisibleNode = null;
        int previousNodeLevel = -1;
        int iteration = 0;
        
        // Variables to track level reduction case
        boolean needsScroll = false;
        int finalBreadcrumbHeight = 0;
        int levelReductionFirstVisibleNodeIndex = -1;
        
        while (true) {
            iteration++;
            System.out.println("\n--- Iteration " + iteration + " ---");
            
            int effectiveViewportY = viewRect.y + workingBreadcrumbHeight;
            int firstFullyVisibleNodeIndex = Math.max(0, (effectiveViewportY + navButtons.rowHeight - 1) / navButtons.rowHeight);
            
            System.out.println("workingBreadcrumbHeight: " + workingBreadcrumbHeight);
            System.out.println("effectiveViewportY: " + effectiveViewportY);
            System.out.println("firstFullyVisibleNodeIndex: " + firstFullyVisibleNodeIndex);
            
            if (firstFullyVisibleNodeIndex >= visibleNodes.size()) {
                System.out.println("Index >= visibleNodes.size(), returning empty state");
                return new BreadcrumbState(java.util.Collections.emptyList(), 0, false, -1);
            }

            TreeNode firstFullyVisibleNode = visibleNodes.get(firstFullyVisibleNodeIndex).node;
            System.out.println("firstFullyVisibleNode: " + firstFullyVisibleNode.title);
            
            if (firstFullyVisibleNode == root) {
                System.out.println("firstFullyVisibleNode == root, returning empty state");
                return new BreadcrumbState(java.util.Collections.emptyList(), 0, false, -1);
            }

            if (firstFullyVisibleNode == previousFirstVisibleNode) {
                System.out.println("firstFullyVisibleNode == previousFirstVisibleNode, breaking");
                break;
            }

            java.util.List<TreeNode> breadcrumbNodes = new java.util.ArrayList<>();
            TreeNode current = firstFullyVisibleNode.parent;
            while (current != null) {
                breadcrumbNodes.add(0, current);
                current = current.parent;
            }

            int newBreadcrumbHeight = breadcrumbNodes.size() * navButtons.rowHeight;
            int currentNodeLevel = calculateNodeDepth(firstFullyVisibleNode);
            
            System.out.println("currentNodeLevel: " + currentNodeLevel + ", previousNodeLevel: " + previousNodeLevel);
            System.out.println("newBreadcrumbHeight: " + newBreadcrumbHeight);
            
            if (previousNodeLevel >= 0 && currentNodeLevel < previousNodeLevel) {
                System.out.println("LEVEL REDUCTION DETECTED!");
                System.out.println("previousFirstVisibleNode: " + (previousFirstVisibleNode != null ? previousFirstVisibleNode.title : "null"));
                System.out.println("currentFirstVisibleNode: " + firstFullyVisibleNode.title);
                System.out.println("Setting levelReductionFirstVisibleNodeIndex to: " + firstFullyVisibleNodeIndex);
                
                // Use the current firstFullyVisibleNode as the first visible content node
                // This is the node that should appear at the top of the content area
                needsScroll = true;
                finalBreadcrumbHeight = newBreadcrumbHeight;
                levelReductionFirstVisibleNodeIndex = firstFullyVisibleNodeIndex;
                break;
            }
            
            if (iteration > 10) {
                System.out.println("Max iterations reached, breaking");
                break;
            }
            
            workingBreadcrumbHeight = newBreadcrumbHeight;
            previousNodeLevel = currentNodeLevel;
            previousFirstVisibleNode = firstFullyVisibleNode;
            System.out.println("Updated previousFirstVisibleNode to: " + previousFirstVisibleNode.title);
        }

        // Build final breadcrumb path
        java.util.List<TreeNode> breadcrumbNodes = new java.util.ArrayList<>();
        TreeNode current = previousFirstVisibleNode.parent;
        while (current != null) {
            breadcrumbNodes.add(0, current);
            current = current.parent;
        }

        int finalHeight = needsScroll ? finalBreadcrumbHeight : breadcrumbNodes.size() * navButtons.rowHeight;
        
        System.out.println("\n=== FINAL RESULT ===");
        System.out.println("needsScroll: " + needsScroll);
        System.out.println("finalHeight: " + finalHeight);
        System.out.println("levelReductionFirstVisibleNodeIndex: " + levelReductionFirstVisibleNodeIndex);
        if (levelReductionFirstVisibleNodeIndex >= 0 && levelReductionFirstVisibleNodeIndex < visibleNodes.size()) {
            System.out.println("Node at levelReductionFirstVisibleNodeIndex: " + visibleNodes.get(levelReductionFirstVisibleNodeIndex).node.title);
        }
        System.out.println("=== calculateBreadcrumbState END ===\n");
        
        return new BreadcrumbState(breadcrumbNodes, finalHeight, needsScroll, levelReductionFirstVisibleNodeIndex);
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
}