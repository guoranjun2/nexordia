package org.freeplane.view.swing.map.outline;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

class BreadcrumbPanel extends JPanel {
    private final ScrollableTreePanel treePanel;
    private final JScrollPane scrollPane;
    private final TreeSelection selection;
    private int currentBreadcrumbHeight = 0;

    public BreadcrumbPanel(ScrollableTreePanel treePanel, JScrollPane scrollPane, TreeSelection selection) {
        this.treePanel = treePanel;
        this.scrollPane = scrollPane;
        this.selection = selection;
        
        setLayout(null);
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    public void update(BreadcrumbState state) {
        removeAll();

        List<TreeNode> breadcrumbNodes = state.breadcrumbNodes;
        currentBreadcrumbHeight = state.breadcrumbHeight;
        
        treePanel.setBreadcrumbAreaHeight(currentBreadcrumbHeight);

        for (int i = 0; i < breadcrumbNodes.size(); i++) {
            TreeNode node = breadcrumbNodes.get(i);
            int depth = getNodeDepth(node);
            int y = i * treePanel.navButtons.rowHeight;

            int actionX;
            if (depth == 0) {
                actionX = treePanel.navButtons.buttonAreaWidth - treePanel.navButtons.indent;
            } else {
                actionX = (depth * treePanel.navButtons.indent) + treePanel.navButtons.buttonAreaWidth - treePanel.navButtons.indent;
            }

            JButton breadcrumbButton = new JButton(node.title);
            breadcrumbButton.setBounds(actionX, y, breadcrumbButton.getPreferredSize().width, treePanel.navButtons.rowHeight);

            final TreeNode nodeToSelect = node;
            final int rowIndex = i;
            breadcrumbButton.addActionListener(e -> {
                treePanel.setSelectedNodeId(nodeToSelect.id);
                treePanel.requestFocusInWindow();
            });

            breadcrumbButton.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    showNavigationButtonsForBreadcrumb(nodeToSelect, rowIndex);
                }
            });

            add(breadcrumbButton);
            
            if (selection.isSelected(node)) {
                SelectionIndicator selectionIndicator = new SelectionIndicator(treePanel.selectionIcon, breadcrumbButton);
                add(selectionIndicator);
            }
        }

        revalidate();
        repaint();
        
        if (treePanel.hoveredNode != null && !treePanel.hoveredNode.children.isEmpty()) {
            boolean isInBreadcrumb = breadcrumbNodes.contains(treePanel.hoveredNode);
            
            if (isInBreadcrumb) {
                int hoveredRowIndex = -1;
                for (int i = 0; i < breadcrumbNodes.size(); i++) {
                    if (breadcrumbNodes.get(i) == treePanel.hoveredNode) {
                        hoveredRowIndex = i;
                        break;
                    }
                }
                if (hoveredRowIndex >= 0) {
                    treePanel.navButtons.attachToNode(treePanel.hoveredNode, this, true, hoveredRowIndex, currentBreadcrumbHeight, treePanel);
                }
            } else {
                boolean buttonsCurrentlyVisible = treePanel.navButtons.expandBtn.isVisible() || 
                                                treePanel.navButtons.collapseBtn.isVisible() ||
                                                treePanel.navButtons.expandMoreBtn.isVisible() || 
                                                treePanel.navButtons.reduceBtn.isVisible();
                
                if (buttonsCurrentlyVisible) {
                    treePanel.navButtons.attachToNode(treePanel.hoveredNode, treePanel, false, -1, currentBreadcrumbHeight, treePanel);
                }
            }
        }
    }

    public Rectangle calculateBounds() {
        int width = scrollPane.getViewport().getWidth();
        return new Rectangle(0, 0, width, currentBreadcrumbHeight);
    }

    public int getCurrentHeight() {
        return currentBreadcrumbHeight;
    }

    private int getNodeDepth(TreeNode node) {
        return treePanel.calculateNodeDepth(node);
    }

    private void showNavigationButtonsForBreadcrumb(TreeNode node, int rowIndex) {
        if (node.children.isEmpty()) {
            return;
        }

        treePanel.navButtons.attachToNode(node, this, true, rowIndex, currentBreadcrumbHeight, treePanel);
    }

    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        
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
            
            int iconX = targetButton.getX() - icon.getIconWidth();
            setBounds(iconX, targetButton.getY(), icon.getIconWidth(), treePanel.navButtons.rowHeight);
        }
        
        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            
            int iconY = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g, 0, iconY);
        }
    }
} 