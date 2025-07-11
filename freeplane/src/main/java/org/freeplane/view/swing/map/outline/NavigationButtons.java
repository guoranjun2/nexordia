package org.freeplane.view.swing.map.outline;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;

class NavigationButtons {
    final JButton expandBtn;
    final JButton collapseBtn;
    final JButton expandMoreBtn;
    final JButton reduceBtn;
    
    private final OutlineGeometry geometry;
    private JPanel currentParent;
    
    NavigationButtons(OutlineGeometry geometry) {
        this.geometry = geometry;
        
        expandBtn = new JButton("▶");
        collapseBtn = new JButton("◀");
        expandMoreBtn = new JButton("▼");
        reduceBtn = new JButton("▲");
        
        configureNavigationButtons();
    }
    
    private void configureNavigationButtons() {
        configureNavButton(expandBtn);
        configureNavButton(collapseBtn);
        configureNavButton(expandMoreBtn);
        configureNavButton(reduceBtn);
    }
    
    private void configureNavButton(JButton button) {
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setFont(button.getFont().deriveFont(10f));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setVisible(false);
    }
    
    public void attachToNode(TreeNode node, JPanel targetPanel, boolean isBreadcrumb, int rowIndex, int breadcrumbAreaHeight, ScrollableTreePanel treePanel) {
        if (node.children.isEmpty()) {
            hide();
            return;
        }

        // Remove buttons from current parent
        detachFromCurrentParent();

        // Add buttons to target panel
        targetPanel.add(expandBtn);
        targetPanel.add(collapseBtn);
        targetPanel.add(expandMoreBtn);
        targetPanel.add(reduceBtn);
        
        currentParent = targetPanel;

        // Calculate position and show appropriate buttons
        int y, depth, baseX;
        if (isBreadcrumb) {
            y = rowIndex * geometry.rowHeight;
            depth = calculateNodeDepth(node, treePanel.root);
            int textButtonX = geometry.calculateTextButtonX(depth);
            baseX = Math.max(0, textButtonX - geometry.navButtonsTotalWidth);
        } else {
            FlatNode flatNode = treePanel.visibleState.getFlatNode(node);
            if (flatNode == null) return;
            int nodeIndex = treePanel.visibleState.findNodeIndexInVisibleList(node);
            int breadcrumbNodeCount = breadcrumbAreaHeight / geometry.rowHeight;
            int contentAreaIndex = nodeIndex - breadcrumbNodeCount;
            y = breadcrumbAreaHeight + contentAreaIndex * geometry.rowHeight;
            depth = flatNode.depth;
            baseX = geometry.calculateNavigationButtonBaseX(flatNode);
        }

        removeAllActionListeners();

        final boolean isInBreadcrumb = isBreadcrumb;
        if (!node.isExpanded()) {
            showSingleButton(expandBtn, baseX, y, () -> {
                node.applyExpansionLevel(1);
                treePanel.refreshWithBreadcrumbs();
            });
        } else {
            showExpandedButtons(node, baseX, y, depth, isInBreadcrumb, treePanel);
        }
    }
    
    private void detachFromCurrentParent() {
        hide();
        if (currentParent != null) {
            if (expandBtn.getParent() == currentParent) {
                currentParent.remove(expandBtn);
            }
            if (collapseBtn.getParent() == currentParent) {
                currentParent.remove(collapseBtn);
            }
            if (expandMoreBtn.getParent() == currentParent) {
                currentParent.remove(expandMoreBtn);
            }
            if (reduceBtn.getParent() == currentParent) {
                currentParent.remove(reduceBtn);
            }
        }
    }
    
    private void showExpandedButtons(TreeNode node, int baseX, int y, int depth, boolean isInBreadcrumb, ScrollableTreePanel treePanel) {
        hide();

        if (depth > 0) {
            collapseBtn.setBounds(baseX, y, geometry.navButtonWidth, geometry.rowHeight);
            collapseBtn.addActionListener(e -> {
                node.applyExpansionLevel(0);
                treePanel.refreshWithBreadcrumbs();
                treePanel.requestFocusInWindow();
            });
            collapseBtn.setVisible(true);
        }

        int expandX = depth == 0 ? baseX : baseX + geometry.navButtonWidth;
        expandMoreBtn.setBounds(expandX, y, geometry.navButtonWidth, geometry.rowHeight);
        expandMoreBtn.addActionListener(e -> {
            int currentLevel = node.getMaxExpansionDepth();
            node.applyExpansionLevel(currentLevel + 1);
            treePanel.refreshWithBreadcrumbs();
            treePanel.requestFocusInWindow();
        });
        expandMoreBtn.setVisible(true);

        int reduceX = depth == 0 ? baseX + geometry.navButtonWidth : baseX + (2 * geometry.navButtonWidth);
        reduceBtn.setBounds(reduceX, y, geometry.navButtonWidth, geometry.rowHeight);
        reduceBtn.addActionListener(e -> {
            int currentLevel = node.getMaxExpansionDepth();
            if (currentLevel > 0) {
                node.applyExpansionLevel(currentLevel - 1);
                treePanel.refreshWithBreadcrumbs();
                treePanel.requestFocusInWindow();
            }
        });
        reduceBtn.setVisible(true);
    }
    
    private void showSingleButton(JButton button, int baseX, int y, Runnable action) {
        hide();
        button.setBounds(baseX, y, geometry.navButtonWidth, geometry.rowHeight);
        button.addActionListener(e -> action.run());
        button.setVisible(true);
    }
    
    private int calculateNodeDepth(TreeNode node, TreeNode root) {
        int depth = 0;
        TreeNode current = node;
        while (current != root) {
            current = current.parent;
            depth++;
        }
        return depth;
    }
    
    private FlatNode findFlatNode(TreeNode node, java.util.List<FlatNode> visibleNodes) {
        for (FlatNode flat : visibleNodes) {
            if (flat.node == node) {
                return flat;
            }
        }
        return null;
    }
    
    private int findNodeIndexInVisibleList(TreeNode node, java.util.List<FlatNode> visibleNodes) {
        for (int i = 0; i < visibleNodes.size(); i++) {
            if (visibleNodes.get(i).node == node) {
                return i;
            }
        }
        return -1;
    }
    

    
    public void hide() {
        expandBtn.setVisible(false);
        collapseBtn.setVisible(false);
        expandMoreBtn.setVisible(false);
        reduceBtn.setVisible(false);
    }
    
    void hideNavigationButtons() {
        hide();
    }
    
    void removeAllActionListeners() {
        removeActionListeners(expandBtn);
        removeActionListeners(collapseBtn);
        removeActionListeners(expandMoreBtn);
        removeActionListeners(reduceBtn);
    }
    
    private void removeActionListeners(JButton button) {
        for (ActionListener listener : button.getActionListeners()) {
            button.removeActionListener(listener);
        }
    }
} 