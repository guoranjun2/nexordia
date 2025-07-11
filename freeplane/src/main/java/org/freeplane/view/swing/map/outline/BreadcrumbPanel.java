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
    private TreeSelection selection;
    private int currentBreadcrumbHeight = 0;
    private List<TreeNode> currentBreadcrumbNodes = new java.util.ArrayList<>();

    public BreadcrumbPanel() {
        setLayout(null);
        setBackground(Color.WHITE);
        setOpaque(true);
    }

    public void initialize(ScrollableTreePanel treePanel, JScrollPane scrollPane, TreeSelection selection) {
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

        if (treePanel.hoveredNode != null && !treePanel.hoveredNode.children.isEmpty()) {
            boolean isInBreadcrumb = currentBreadcrumbNodes.contains(treePanel.hoveredNode);

            if (isInBreadcrumb) {
                int hoveredRowIndex = -1;
                for (int i = 0; i < currentBreadcrumbNodes.size(); i++) {
                    if (currentBreadcrumbNodes.get(i) == treePanel.hoveredNode) {
                        hoveredRowIndex = i;
                        break;
                    }
                }
                if (hoveredRowIndex >= 0) {
                    treePanel.attachNavigationNode(treePanel.hoveredNode, true, hoveredRowIndex, currentBreadcrumbHeight);
                }
            } else {
                boolean buttonsCurrentlyVisible = treePanel.navButtons.expandBtn.isVisible() ||
                                                treePanel.navButtons.collapseBtn.isVisible() ||
                                                treePanel.navButtons.expandMoreBtn.isVisible() ||
                                                treePanel.navButtons.reduceBtn.isVisible();

                if (buttonsCurrentlyVisible) {
                    treePanel.attachNavigationNode(treePanel.hoveredNode, false, -1, currentBreadcrumbHeight);
                }
            }
        }

        revalidate();
        repaint();
	}

    public Rectangle calculateBounds() {
        int width = scrollPane.getViewport().getWidth();
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