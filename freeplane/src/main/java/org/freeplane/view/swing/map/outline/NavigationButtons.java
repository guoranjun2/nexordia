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
    
    final int arrowAreaWidth;
    final int indent;
    final int rowHeight;
    
    final int navButtonWidth;
    final int navButtonsTotalWidth;
    final int standardGap;
    final int buttonAreaWidth;
    final int iconDiameter;
    
    private JPanel currentParent;
    
    NavigationButtons() {
        expandBtn = new JButton("▶");
        collapseBtn = new JButton("◀");
        expandMoreBtn = new JButton("▼");
        reduceBtn = new JButton("▲");
        
        configureNavigationButtons();
        
        final Dimension preferredButtonSize = expandBtn.getPreferredSize();
        this.arrowAreaWidth = Math.round(preferredButtonSize.width * 60 / 13);
        this.indent = arrowAreaWidth / 2;
        this.rowHeight = Math.round(preferredButtonSize.height * 30 / 17);
        
        this.navButtonWidth = Math.round(preferredButtonSize.width * 20 / 13);
        this.navButtonsTotalWidth = 3 * navButtonWidth;
        this.standardGap = Math.round(preferredButtonSize.width * 12 / 13);
        this.buttonAreaWidth = navButtonsTotalWidth + (2 * standardGap);
        this.iconDiameter = Math.round(preferredButtonSize.width * 10 / 13);
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
            y = rowIndex * rowHeight;
            depth = calculateNodeDepth(node, treePanel.root);
            int textButtonX;
            if (depth == 0) {
                textButtonX = buttonAreaWidth - indent;
            } else {
                textButtonX = (depth * indent) + buttonAreaWidth - indent;
            }
            baseX = Math.max(0, textButtonX - navButtonsTotalWidth);
        } else {
            FlatNode flatNode = findFlatNode(node, treePanel.visibleNodes);
            if (flatNode == null) return;
            int nodeIndex = findNodeIndexInVisibleList(node, treePanel.visibleNodes);
            int breadcrumbNodeCount = breadcrumbAreaHeight / rowHeight;
            int contentAreaIndex = nodeIndex - breadcrumbNodeCount;
            y = breadcrumbAreaHeight + contentAreaIndex * rowHeight;
            depth = flatNode.depth;
            baseX = calculateBaseX(flatNode);
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
            collapseBtn.setBounds(baseX, y, navButtonWidth, rowHeight);
            collapseBtn.addActionListener(e -> {
                node.applyExpansionLevel(0);
                treePanel.refreshWithBreadcrumbs();
                treePanel.requestFocusInWindow();
            });
            collapseBtn.setVisible(true);
        }

        int expandX = depth == 0 ? baseX : baseX + navButtonWidth;
        expandMoreBtn.setBounds(expandX, y, navButtonWidth, rowHeight);
        expandMoreBtn.addActionListener(e -> {
            int currentLevel = node.getMaxExpansionDepth();
            node.applyExpansionLevel(currentLevel + 1);
            treePanel.refreshWithBreadcrumbs();
            treePanel.requestFocusInWindow();
        });
        expandMoreBtn.setVisible(true);

        int reduceX = depth == 0 ? baseX + navButtonWidth : baseX + (2 * navButtonWidth);
        reduceBtn.setBounds(reduceX, y, navButtonWidth, rowHeight);
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
        button.setBounds(baseX, y, navButtonWidth, rowHeight);
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
    
    private int calculateBaseX(FlatNode flat) {
        // Calculate where the text button would be positioned
        int textButtonX;
        if (flat.depth == 0) {
            textButtonX = buttonAreaWidth - indent;
        } else {
            textButtonX = (flat.depth * indent) + buttonAreaWidth - indent;
        }
        
        // Position navigation buttons just to the left of the text button
        int baseX = textButtonX - navButtonsTotalWidth;
        return Math.max(0, baseX);
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