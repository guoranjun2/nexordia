package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

class BreadcrumbPanel extends JPanel {
    private ScrollableTreePanel treePanel;
    private JScrollPane scrollPane;
    private OutlineSelection selection;
    private int currentBreadcrumbHeight = 0;
    private List<TreeNode> currentBreadcrumbNodes = new java.util.ArrayList<>();

    public BreadcrumbPanel() {
        setLayout(null);
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    public void initialize(ScrollableTreePanel treePanel, JScrollPane scrollPane, OutlineSelection selection) {
        this.treePanel = treePanel;
        this.scrollPane = scrollPane;
        this.selection = selection;
    }

    public void update(BreadcrumbState state) {
        removeAll();

        currentBreadcrumbHeight = state.breadcrumbHeight;
        this.currentBreadcrumbNodes = new java.util.ArrayList<>(state.breadcrumbNodes);

        treePanel.setBreadcrumbAreaHeight(currentBreadcrumbHeight);
        updateNavigationButtons();
    }

	void updateNavigationButtons() {
		for (int i = 0; i < currentBreadcrumbNodes.size(); i++) {
            TreeNode node = currentBreadcrumbNodes.get(i);
            int depth = getNodeDepth(node);
            int y = i * treePanel.geometry.rowHeight;

            int actionX = treePanel.geometry.calculateTextButtonX(depth);

            JButton breadcrumbButton = new JButton();
            breadcrumbButton.setFont(breadcrumbButton.getFont().deriveFont(8f));
            breadcrumbButton.setText(node.title);
            breadcrumbButton.setBounds(actionX, y, breadcrumbButton.getPreferredSize().width, treePanel.geometry.rowHeight);
            
            // Store the node reference with the button for selection checking
            breadcrumbButton.putClientProperty("treeNode", node);

            final TreeNode nodeToSelect = node;
            final int rowIndex = i;
            breadcrumbButton.addActionListener(e -> {
                treePanel.selectNodeById(nodeToSelect.id);
                treePanel.requestFocusInWindow();
            });

            breadcrumbButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    showNavigationButtonsForBreadcrumb(nodeToSelect, rowIndex);
                }
            });

            add(breadcrumbButton);

        }

        TreeNode hoveredNode = treePanel.getVisibleState().getHoveredNode();
        if (hoveredNode != null && !hoveredNode.children.isEmpty()) {
            boolean isInBreadcrumb = treePanel.breadcrumbPath.isNodeInBreadcrumbPath(hoveredNode, currentBreadcrumbNodes);

            if (isInBreadcrumb) {
                int hoveredRowIndex = treePanel.breadcrumbPath.findNodeIndexInBreadcrumbPath(hoveredNode, currentBreadcrumbNodes);
                if (hoveredRowIndex >= 0) {
                    treePanel.attachNavigationNode(hoveredNode, true, hoveredRowIndex, currentBreadcrumbHeight);
                }
            } else {
                boolean buttonsCurrentlyVisible = treePanel.navButtons.expandBtn.isVisible() ||
                                                treePanel.navButtons.collapseBtn.isVisible() ||
                                                treePanel.navButtons.expandMoreBtn.isVisible() ||
                                                treePanel.navButtons.reduceBtn.isVisible();

                if (buttonsCurrentlyVisible) {
                    treePanel.attachNavigationNode(hoveredNode, false, -1, currentBreadcrumbHeight);
                }
            }
        }

        revalidate();
        repaint();
	}


    public Rectangle calculateBounds() {
        int width = treePanel.viewport != null ? treePanel.viewport.getViewportWidth() : scrollPane.getViewport().getWidth();
        return new Rectangle(0, 0, width, currentBreadcrumbHeight);
    }

    public int getCurrentHeight() {
        return currentBreadcrumbHeight;
    }

    public List<TreeNode> getCurrentBreadcrumbNodes() {
        return new java.util.ArrayList<>(currentBreadcrumbNodes);
    }

    private int getNodeDepth(TreeNode node) {
        return treePanel.calculateNodeDepth(node);
    }

    private void showNavigationButtonsForBreadcrumb(TreeNode node, int rowIndex) {
        if (node.children.isEmpty()) {
            return;
        }

        treePanel.attachNavigationNode(node, true, rowIndex, currentBreadcrumbHeight);
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);

        // Paint selection indicators for selected breadcrumb buttons
        for (java.awt.Component comp : getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                TreeNode buttonNode = (TreeNode) btn.getClientProperty("treeNode");
                if (buttonNode != null && selection.isSelected(buttonNode)) {
                    javax.swing.Icon icon = treePanel.selectionIcon;
                    
                    java.awt.Point iconPosition = treePanel.getNodePositioning().calculateSelectionIconPosition(buttonNode, comp.getBounds());
                    if (iconPosition != null) {
                        icon.paintIcon(this, g, iconPosition.x, iconPosition.y);
                    }
                }
            }
        }

        // Debug: Paint a horizontal line at the bottom of the breadcrumb panel
        if (currentBreadcrumbHeight > 0) {
            g.setColor(java.awt.Color.RED);
            g.drawLine(0, currentBreadcrumbHeight - 1, getWidth(), currentBreadcrumbHeight - 1);
        }
    }

    private class SelectionIndicator extends JPanel {
        private final SelectionCircleIcon icon;
        private final JButton targetButton;

        SelectionIndicator(SelectionCircleIcon icon, JButton targetButton) {
            this.icon = icon;
            this.targetButton = targetButton;
            setOpaque(false);
            setFocusable(false);

            // Position will be updated in updatePosition() after layout is complete
            setBounds(0, 0, icon.getIconWidth(), treePanel.geometry.rowHeight);
        }
        
        private void updatePosition() {
            int iconX = targetButton.getX() - icon.getIconWidth();
            setBounds(iconX, targetButton.getY(), icon.getIconWidth(), treePanel.geometry.rowHeight);
        }

        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);

            int iconY = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g, 0, iconY);
        }
    }
}