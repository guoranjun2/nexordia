/*
 * Created on 8 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class OutlinePane extends JPanel {
    private final JScrollPane treeScrollPane;
    private final ScrollableTreePanel treePanel;
    private final JPanel breadcrumbPanel;
    private int currentBreadcrumbHeight = 0;

    public OutlinePane(ScrollableTreePanel treePanel) {
        this.treePanel = treePanel;
        this.treeScrollPane = new JScrollPane(treePanel);
        
        // Set up the scroll pane connection
        treePanel.setScrollPane(this.treeScrollPane);

        setLayout(new BorderLayout(0, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public void layoutContainer(Container parent) {
                super.layoutContainer(parent);
                treeScrollPane.validate();
                breadcrumbPanel.setBounds(calculateBreadcrumbBounds());
            }
        });

        breadcrumbPanel = new JPanel();
        breadcrumbPanel.setLayout(null);
        breadcrumbPanel.setBackground(Color.WHITE);
        breadcrumbPanel.setOpaque(true);

        add(breadcrumbPanel);
        add(treeScrollPane, BorderLayout.CENTER);
        
        setupScrollListeners();
        performInitialSetup();
    }
    
    private void setupScrollListeners() {
        // Add scroll bar adjustment listener
        treeScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            updateBreadcrumbs();
            treePanel.updateVisibleBlocks();
        });
        
        // Add viewport resize listener
        treeScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateBreadcrumbs();
                treePanel.updateVisibleBlocks();
            }
        });
    }
    
    private void performInitialSetup() {
        SwingUtilities.invokeLater(() -> {
            updateBreadcrumbs();
            treePanel.updateVisibleBlocks();
            treePanel.requestFocusInWindow();
        });
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    private Rectangle calculateBreadcrumbBounds() {
        int width = treeScrollPane.getViewport().getWidth();
        return new Rectangle(0, 0, width, currentBreadcrumbHeight);
    }

    public void updateBreadcrumbs() {
        breadcrumbPanel.removeAll();

        List<TreeNode> breadcrumbNodes = calculateBreadcrumbNodes();

        currentBreadcrumbHeight = breadcrumbNodes.size() * treePanel.navButtons.rowHeight;

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

            breadcrumbPanel.add(breadcrumbButton);
            
            // Paint selection circle for selected node in breadcrumb
            if (treePanel.getSelectedNode() != null && treePanel.getSelectedNode().id.equals(node.id)) {
                // We'll add a custom component to paint the selection circle
                SelectionIndicator selectionIndicator = new SelectionIndicator(treePanel.selectionIcon, breadcrumbButton);
                breadcrumbPanel.add(selectionIndicator);
            }
        }

        breadcrumbPanel.revalidate();
        breadcrumbPanel.repaint();

        revalidate();
        
        // Check if hovered node should have navigation buttons in breadcrumb area
        if (treePanel.hoveredNode != null && !treePanel.hoveredNode.children.isEmpty()) {
            boolean isInBreadcrumb = breadcrumbNodes.contains(treePanel.hoveredNode);
            
            if (isInBreadcrumb) {
                // Find the row index of the hovered node in breadcrumb
                int hoveredRowIndex = -1;
                for (int i = 0; i < breadcrumbNodes.size(); i++) {
                    if (breadcrumbNodes.get(i) == treePanel.hoveredNode) {
                        hoveredRowIndex = i;
                        break;
                    }
                }
                if (hoveredRowIndex >= 0) {
                    treePanel.moveNavigationButtonsTo(breadcrumbPanel, treePanel.hoveredNode, hoveredRowIndex, true);
                }
            } else {
                // Node is in main content, check if buttons should be positioned there
                boolean buttonsCurrentlyVisible = treePanel.navButtons.expandBtn.isVisible() || 
                                                treePanel.navButtons.collapseBtn.isVisible() ||
                                                treePanel.navButtons.expandMoreBtn.isVisible() || 
                                                treePanel.navButtons.reduceBtn.isVisible();
                
                if (buttonsCurrentlyVisible) {
                    treePanel.moveNavigationButtonsTo(treePanel, treePanel.hoveredNode, -1, false);
                }
            }
        }
    }

    private int getNodeDepth(TreeNode node) {
        int depth = 0;
        TreeNode current = node;
        while (current != treePanel.root) {
            current = current.parent;
            depth++;
        }
        return depth;
    }

    private List<TreeNode> calculateBreadcrumbNodes() {
        if (treePanel.visibleNodes.isEmpty()) {
            return Collections.emptyList();
        }

        Rectangle viewRect = treeScrollPane.getViewport().getViewRect();

        // Start with height = 0 and identify first visible node
        int workingBreadcrumbHeight = 0;
        TreeNode previousFirstVisibleNode = null;
        int previousNodeLevel = -1;
        int iteration = 0;
        
        while (true) {
            iteration++;
            
            // Calculate effective viewport position accounting for current breadcrumb height
            int effectiveViewportY = viewRect.y + workingBreadcrumbHeight;
            int firstFullyVisibleNodeIndex = Math.max(0, (effectiveViewportY + treePanel.navButtons.rowHeight - 1) / treePanel.navButtons.rowHeight);
            
            if (firstFullyVisibleNodeIndex >= treePanel.visibleNodes.size()) {
                return Collections.emptyList();
            }

            TreeNode firstFullyVisibleNode = treePanel.visibleNodes.get(firstFullyVisibleNodeIndex).node;
            
            if (firstFullyVisibleNode == treePanel.root) {
                return Collections.emptyList();
            }

            // If the first visible node hasn't changed, we're done
            if (firstFullyVisibleNode == previousFirstVisibleNode) {
                break;
            }

            // Build breadcrumb path from this first visible node
            List<TreeNode> breadcrumbNodes = new ArrayList<>();
            TreeNode current = firstFullyVisibleNode.parent;
            while (current != null) {
                breadcrumbNodes.add(0, current);
                current = current.parent;
            }

            // Recalculate breadcrumb height
            int newBreadcrumbHeight = breadcrumbNodes.size() * treePanel.navButtons.rowHeight;
            int currentNodeLevel = getNodeDepth(firstFullyVisibleNode);
            
            // If node level is decreasing (going backwards to shallower node), scroll view to accommodate reduced breadcrumb height
            if (previousNodeLevel >= 0 && currentNodeLevel < previousNodeLevel) {
                // Calculate the previous first visible node index (the deeper node we want to keep visible)
                int previousFirstVisibleNodeIndex = -1;
                for (int i = 0; i < treePanel.visibleNodes.size(); i++) {
                    if (treePanel.visibleNodes.get(i).node == previousFirstVisibleNode) {
                        previousFirstVisibleNodeIndex = i;
                        break;
                    }
                }
                
                if (previousFirstVisibleNodeIndex >= 0) {
                    // Calculate where this node should be positioned with the reduced breadcrumb height
                    int desiredViewportY = (previousFirstVisibleNodeIndex * treePanel.navButtons.rowHeight) - newBreadcrumbHeight;
                    desiredViewportY = Math.max(0, desiredViewportY); // Don't scroll beyond beginning
                    
                    // Scroll to the desired position
                    treeScrollPane.getViewport().setViewPosition(new java.awt.Point(0, desiredViewportY));
                    
                    // Use the reduced breadcrumb configuration
                    workingBreadcrumbHeight = newBreadcrumbHeight;
                }
                break;
            }
            
            // Safety check to prevent infinite loops
            if (iteration > 10) {
                break;
            }
            
            workingBreadcrumbHeight = newBreadcrumbHeight;
            previousNodeLevel = currentNodeLevel;
            
            // Remember this first visible node for next iteration
            previousFirstVisibleNode = firstFullyVisibleNode;
        }

        // Build final breadcrumb path
        List<TreeNode> breadcrumbNodes = new ArrayList<>();
        TreeNode current = previousFirstVisibleNode.parent;
        while (current != null) {
            breadcrumbNodes.add(0, current);
            current = current.parent;
        }

        return breadcrumbNodes;
    }


    private void showNavigationButtonsForBreadcrumb(TreeNode node, int rowIndex) {
        if (node.children.isEmpty()) {
            return;
        }

        treePanel.moveNavigationButtonsTo(breadcrumbPanel, node, rowIndex, true);
    }
    
    private class SelectionIndicator extends JPanel {
        private final SelectionCircleIcon icon;
        private final JButton targetButton;
        
        SelectionIndicator(SelectionCircleIcon icon, JButton targetButton) {
            this.icon = icon;
            this.targetButton = targetButton;
            setOpaque(false);
            setFocusable(false);
            
            // Position this component to cover the area where we want to paint the icon
            int iconX = targetButton.getX() - icon.getIconWidth();
            setBounds(iconX, targetButton.getY(), icon.getIconWidth(), treePanel.navButtons.rowHeight);
        }
        
        @Override
        protected void paintComponent(java.awt.Graphics g) {
            super.paintComponent(g);
            
            // Paint the icon at the left edge of this component
            int iconY = (getHeight() - icon.getIconHeight()) / 2;
            icon.paintIcon(this, g, 0, iconY);
        }
    }
}