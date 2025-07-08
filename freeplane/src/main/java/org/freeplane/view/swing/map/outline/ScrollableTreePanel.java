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
    final int blockSize;
    List<FlatNode> visibleNodes = new ArrayList<>();
    final Map<Integer, BlockPanel> blockPanels = new HashMap<>();
    JScrollPane scrollPane;
    String selectedNodeId;
    private int breadcrumbAreaHeight = 0;

    TreeNode hoveredNode;

    public ScrollableTreePanel(TreeNode root) {
		this(root, BLOCK_SIZE);
	}

	ScrollableTreePanel(TreeNode root,int blockSize) {
        super(null);
        this.root = root;
        this.blockSize = blockSize;
        this.selectedNodeId = root.id;
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
                moveNavigationButtonsTo(this, hoveredNode, -1, false);
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
        
        BlockPanel bp = new BlockPanel(visibleNodes.subList(start, end), start, navButtons.rowHeight, navButtons.indent, this, breadcrumbNodeCount);
        
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
        if (nodeId != null && findNodeById(nodeId) != null) {
            this.selectedNodeId = nodeId;
            TreeNode preservedHoveredNode = hoveredNode;
            removeAll();
            blockPanels.clear();
            navButtons.hideNavigationButtons();
            hoveredNode = preservedHoveredNode;
            updateVisibleBlocks();
            SwingUtilities.invokeLater(this::scrollToSelectedNode);
        }
    }

    private TreeNode findNodeById(String id) {
        return findNodeById(root, id);
    }

    private TreeNode findNodeById(TreeNode node, String id) {
        if (node.id.equals(id)) {
            return node;
        }
        for (TreeNode child : node.children) {
            TreeNode found = findNodeById(child, id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private void scrollToSelectedNode() {
        if (scrollPane == null || selectedNodeId == null) {
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
            if (visibleNodes.get(i).node.id.equals(selectedNodeId)) {
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
        return selectedNodeId;
    }

    public TreeNode getSelectedNode() {
        return findNodeById(selectedNodeId);
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
            moveNavigationButtonsTo(this, node, -1, false);
            repaint();
        }
    }

    void moveNavigationButtonsTo(JPanel targetPanel, TreeNode node, int rowIndex, boolean isBreadcrumb) {
        if (node.children.isEmpty()) {
            navButtons.hideNavigationButtons();
            return;
        }

        // Remove buttons from current parent
        navButtons.hideNavigationButtons();
        if (navButtons.expandBtn.getParent() != null) {
            navButtons.expandBtn.getParent().remove(navButtons.expandBtn);
        }
        if (navButtons.collapseBtn.getParent() != null) {
            navButtons.collapseBtn.getParent().remove(navButtons.collapseBtn);
        }
        if (navButtons.expandMoreBtn.getParent() != null) {
            navButtons.expandMoreBtn.getParent().remove(navButtons.expandMoreBtn);
        }
        if (navButtons.reduceBtn.getParent() != null) {
            navButtons.reduceBtn.getParent().remove(navButtons.reduceBtn);
        }

        // Add buttons to target panel
        targetPanel.add(navButtons.expandBtn);
        targetPanel.add(navButtons.collapseBtn);
        targetPanel.add(navButtons.expandMoreBtn);
        targetPanel.add(navButtons.reduceBtn);

        // Calculate position and show appropriate buttons
        int y, depth, baseX;
        if (isBreadcrumb) {
            y = rowIndex * navButtons.rowHeight;
            depth = calculateNodeDepth(node);
            // Position navigation buttons just to the left of breadcrumb text buttons
            // Breadcrumb text buttons are at: buttonAreaWidth - indent (+ depth * indent for non-root)
            int textButtonX;
            if (depth == 0) {
                textButtonX = navButtons.buttonAreaWidth - navButtons.indent;
            } else {
                textButtonX = (depth * navButtons.indent) + navButtons.buttonAreaWidth - navButtons.indent;
            }
            baseX = Math.max(0, textButtonX - navButtons.navButtonsTotalWidth);
        } else {
            FlatNode flatNode = findFlatNode(node);
            if (flatNode == null) return;
            int nodeIndex = findNodeIndexInVisibleList(node);
            int breadcrumbNodeCount = breadcrumbAreaHeight / navButtons.rowHeight;
            int contentAreaIndex = nodeIndex - breadcrumbNodeCount;
            y = breadcrumbAreaHeight + contentAreaIndex * navButtons.rowHeight;
            depth = flatNode.depth;
            baseX = calculateBaseX(flatNode);
        }

        removeActionListeners();

        final boolean isInBreadcrumb = isBreadcrumb;
        if (!node.isExpanded()) {
            showSingleButton(navButtons.expandBtn, baseX, y, () -> {
                node.applyExpansionLevel(1);
                refreshWithBreadcrumbs();
            });
        } else {
            showExpandedButtons(node, baseX, y, depth, isInBreadcrumb);
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

    private void showExpandedButtons(TreeNode node, int baseX, int y, int depth, boolean isInBreadcrumb) {
        navButtons.hideNavigationButtons();

        if (depth > 0) {
            navButtons.collapseBtn.setBounds(baseX, y, navButtons.navButtonWidth, navButtons.rowHeight);
            navButtons.collapseBtn.addActionListener(e -> {
                node.applyExpansionLevel(0);
                refreshWithBreadcrumbs();
                requestFocusInWindow();
            });
            navButtons.collapseBtn.setVisible(true);
        }

        int expandX = depth == 0 ? baseX : baseX + navButtons.navButtonWidth;
        navButtons.expandMoreBtn.setBounds(expandX, y, navButtons.navButtonWidth, navButtons.rowHeight);
        navButtons.expandMoreBtn.addActionListener(e -> {
            int currentLevel = node.getMaxExpansionDepth();
            node.applyExpansionLevel(currentLevel + 1);
            refreshWithBreadcrumbs();
            requestFocusInWindow();
        });
        navButtons.expandMoreBtn.setVisible(true);

        int reduceX = depth == 0 ? baseX + navButtons.navButtonWidth : baseX + (2 * navButtons.navButtonWidth);
        navButtons.reduceBtn.setBounds(reduceX, y, navButtons.navButtonWidth, navButtons.rowHeight);
        navButtons.reduceBtn.addActionListener(e -> {
            int currentLevel = node.getMaxExpansionDepth();
            int minLevel = depth == 0 ? 1 : 0;
            if (currentLevel > minLevel) {
                node.applyExpansionLevel(currentLevel - 1);
            } else {
                node.applyExpansionLevel(minLevel);
            }
            refreshWithBreadcrumbs();
            requestFocusInWindow();
        });
        navButtons.reduceBtn.setVisible(true);
    }

    private int calculateBaseXForDepth(int depth) {
        if (depth == 0) {
            return 0;
        } else {
            return (depth * navButtons.indent) - navButtons.indent;
        }
    }

    private int calculateNodeDepth(TreeNode node) {
        int depth = 0;
        TreeNode current = node;
        while (current != root) {
            current = current.parent;
            depth++;
        }
        return depth;
    }

    private void refreshWithBreadcrumbs() {
        TreeNode preservedHoveredNode = hoveredNode;
        SwingUtilities.invokeLater(() -> {
            // Notify TreeViewPane to update breadcrumbs first
            Container parent = getParent();
            while (parent != null && !(parent instanceof OutlinePane)) {
                parent = parent.getParent();
            }
            if (parent instanceof OutlinePane) {
                ((OutlinePane) parent).updateBreadcrumbs();
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
                            ((OutlinePane) outlinePane).updateBreadcrumbs();
                        }
                    } else {
                        // Node is in main content
                        moveNavigationButtonsTo(this, preservedHoveredNode, -1, false);
                    }
                });
            }
        });
    }

    private int calculateBaseX(FlatNode flat) {
        if (flat.depth == 0) {
            return 0;
        } else {
            return (flat.depth * navButtons.indent) - navButtons.indent;
        }
    }

    private void removeActionListeners() {
        navButtons.removeAllActionListeners();
    }

    private void showSingleButton(JButton button, int baseX, int y, Runnable action) {
        navButtons.hideNavigationButtons();
        // Position single button closer to the node text
        int singleButtonX = baseX + navButtons.navButtonsTotalWidth - navButtons.navButtonWidth;
        button.setBounds(singleButtonX, y, navButtons.navButtonWidth, navButtons.rowHeight);
        button.addActionListener(e -> {
            action.run();
            requestFocusInWindow();
        });
        button.setVisible(true);
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

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
}