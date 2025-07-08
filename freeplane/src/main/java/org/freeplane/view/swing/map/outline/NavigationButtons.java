package org.freeplane.view.swing.map.outline;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;

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
    
    void hideNavigationButtons() {
        expandBtn.setVisible(false);
        collapseBtn.setVisible(false);
        expandMoreBtn.setVisible(false);
        reduceBtn.setVisible(false);
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