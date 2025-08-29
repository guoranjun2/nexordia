package org.freeplane.view.swing.map.outline;

import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.Point;

class NavigationButtons {
    final JButton expandBtn;
    final JButton collapseBtn;
    final JButton expandMoreBtn;
    final JButton reduceBtn;
    
    private final OutlineGeometry geometry;
    private final ExpansionControls expansionControls;
    private JPanel currentParent;
    
    NavigationButtons(OutlineGeometry geometry, ExpansionControls expansionControls) {
        this.geometry = geometry;
        this.expansionControls = expansionControls;
        
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
    
    public void attachToNode(TreeNode node, JPanel targetPanel, boolean isBreadcrumb, int rowIndex, int breadcrumbAreaHeight, NodePositioning nodePositioning) {
        if (node.children.isEmpty()) {
            hide();
            return;
        }

        
        detachFromCurrentParent();

        
        targetPanel.add(expandBtn);
        targetPanel.add(collapseBtn);
        targetPanel.add(expandMoreBtn);
        targetPanel.add(reduceBtn);
        
        currentParent = targetPanel;

        
        Point position = nodePositioning.calculateNavigationButtonPosition(node, isBreadcrumb, rowIndex, breadcrumbAreaHeight);
        if (position == null) return;
        
        int baseX = position.x;
        int y = position.y;
        int depth = nodePositioning.calculateNodeDepth(node);

        removeAllActionListeners();

        final boolean isInBreadcrumb = isBreadcrumb;
        if (!node.isExpanded()) {
            showSingleButton(expandBtn, baseX, y, () -> {
                expansionControls.expandNode(node);
            });
        } else {
            showExpandedButtons(node, baseX, y, depth, isInBreadcrumb);
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
    
    private void showExpandedButtons(TreeNode node, int baseX, int y, int depth, boolean isInBreadcrumb) {
        hide();

        if (depth > 0) {
            collapseBtn.setBounds(baseX, y, geometry.navButtonWidth, geometry.rowHeight);
            collapseBtn.addActionListener(e -> {
                expansionControls.collapseNode(node);
            });
            collapseBtn.setVisible(true);
        }

        int expandX = depth == 0 ? baseX : baseX + geometry.navButtonWidth;
        expandMoreBtn.setBounds(expandX, y, geometry.navButtonWidth, geometry.rowHeight);
        expandMoreBtn.addActionListener(e -> {
            expansionControls.expandNodeMore(node);
        });
        expandMoreBtn.setVisible(true);

        int reduceX = depth == 0 ? baseX + geometry.navButtonWidth : baseX + (2 * geometry.navButtonWidth);
        reduceBtn.setBounds(reduceX, y, geometry.navButtonWidth, geometry.rowHeight);
        reduceBtn.addActionListener(e -> {
            expansionControls.reduceNodeExpansion(node);
        });
        reduceBtn.setVisible(true);
    }
    
    private void showSingleButton(JButton button, int baseX, int y, Runnable action) {
        hide();
        button.setBounds(baseX, y, geometry.navButtonWidth, geometry.rowHeight);
        button.addActionListener(e -> action.run());
        button.setVisible(true);
    }
    
    private void hide() {
        expandBtn.setVisible(false);
        collapseBtn.setVisible(false);
        expandMoreBtn.setVisible(false);
        reduceBtn.setVisible(false);
    }
    
    void hideNavigationButtons() {
        hide();
    }
    
    private void removeAllActionListeners() {
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