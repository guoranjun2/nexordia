/*
 * Created on 8 Jul 2025
 *
 * author dimitry
 */
package org.freeplane.view.swing.map.outline;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class OutlinePane extends JPanel {
    private final JScrollPane treeScrollPane;
    private final ScrollableTreePanel treePanel;
    final BreadcrumbPanel breadcrumbPanel;

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
                breadcrumbPanel.setBounds(breadcrumbPanel.calculateBounds());
            }
        });

        breadcrumbPanel = new BreadcrumbPanel(treePanel, treeScrollPane, treePanel.selection);

        add(breadcrumbPanel);
        add(treeScrollPane, BorderLayout.CENTER);
        
        setupScrollListeners();
        performInitialSetup();
    }
    
    private void setupScrollListeners() {
        // Add scroll bar adjustment listener
        treeScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            BreadcrumbState state = treePanel.calculateBreadcrumbState();
            breadcrumbPanel.update(state);
            
            if (state.needsScroll && state.levelReductionFirstVisibleNodeIndex >= 0) {
                // Level reduction case - use atomic update with specific start node
                treePanel.updateVisibleBlocks(state.levelReductionFirstVisibleNodeIndex);
            } else {
                // Normal scrolling - keep smooth scrolling behavior
                treePanel.updateVisibleBlocks();
            }
        });
        
        // Add viewport resize listener
        treeScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                BreadcrumbState state = treePanel.calculateBreadcrumbState();
                breadcrumbPanel.update(state);
                
                if (state.needsScroll && state.levelReductionFirstVisibleNodeIndex >= 0) {
                    // Level reduction case - use atomic update with specific start node
                    treePanel.updateVisibleBlocks(state.levelReductionFirstVisibleNodeIndex);
                } else {
                    // Normal scrolling - keep smooth scrolling behavior
                    treePanel.updateVisibleBlocks();
                }
            }
        });
    }
    
    private void performInitialSetup() {
        SwingUtilities.invokeLater(() -> {
            BreadcrumbState state = treePanel.calculateBreadcrumbState();
            breadcrumbPanel.update(state);
            
            if (state.needsScroll && state.levelReductionFirstVisibleNodeIndex >= 0) {
                // Level reduction case - use atomic update with specific start node
                treePanel.updateVisibleBlocks(state.levelReductionFirstVisibleNodeIndex);
            } else {
                // Normal scrolling - keep smooth scrolling behavior
                treePanel.updateVisibleBlocks();
            }
            
            treePanel.requestFocusInWindow();
        });
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }
}