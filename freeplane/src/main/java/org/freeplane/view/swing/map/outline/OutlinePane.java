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

class OutlinePane extends JPanel {
    private JScrollPane treeScrollPane;
    private ScrollableTreePanel treePanel;
    private BreadcrumbPanel breadcrumbPanel;

    OutlinePane(TreeNode rootNode) {
        // Create BreadcrumbPanel first
        this.breadcrumbPanel = new BreadcrumbPanel();

        // Create ScrollableTreePanel with BreadcrumbPanel as parameter
        this.treePanel = new ScrollableTreePanel(rootNode, breadcrumbPanel);
        this.treeScrollPane = new JScrollPane(treePanel);

        // Set up the scroll pane connection
        treePanel.setScrollPane(this.treeScrollPane);

        // Wire BreadcrumbPanel with its dependencies
        breadcrumbPanel.initialize(treePanel, treeScrollPane, treePanel.getSelection());

        setLayout(new BorderLayout(0, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public void layoutContainer(Container parent) {
                super.layoutContainer(parent);
                treeScrollPane.validate();
                breadcrumbPanel.setBounds(breadcrumbPanel.calculateBounds());
            }
        });

        add(breadcrumbPanel);
        add(treeScrollPane, BorderLayout.CENTER);

        setupScrollListeners();
    }

    /**
     * Refresh the tree display to reflect changes in the underlying tree structure.
     * Called when nodes are added, removed, or modified.
     */
    void refreshTree() {
        SwingUtilities.invokeLater(() -> {
            treePanel.updateVisibleNodes();
        });
    }

    /**
     * Replace the root node and refresh the entire tree display.
     * Used to switch from placeholder/demo data to real map data.
     *
     * @param newRootNode the new root node for the tree
     */
    void setRootNode(TreeNode newRootNode) {
        if (newRootNode == null) {
            newRootNode = new TreeNode("No Data", "empty");
        }

        // Clean up old tree listeners to prevent memory leaks
        if (treePanel != null) {
            TreeNode oldRoot = treePanel.getRoot();
            if (oldRoot != null) {
                NodeTreeFactory.cleanupTree(oldRoot);
            }
        }

        // Remove old components
        remove(treeScrollPane);
        remove(breadcrumbPanel);

        // Create new breadcrumb panel (avoid state persistence)
        BreadcrumbPanel newBreadcrumbPanel = new BreadcrumbPanel();

        // Create new tree panel like in constructor (avoid updateRoot complexity)
        ScrollableTreePanel newTreePanel = new ScrollableTreePanel(newRootNode, newBreadcrumbPanel);
        JScrollPane newScrollPane = new JScrollPane(newTreePanel);

        // Set up the scroll pane connection
        newTreePanel.setScrollPane(newScrollPane);

        // Wire BreadcrumbPanel with its dependencies
        newBreadcrumbPanel.initialize(newTreePanel, newScrollPane, newTreePanel.getSelection());

        // Update references
        this.treePanel = newTreePanel;
        this.treeScrollPane = newScrollPane;
        this.breadcrumbPanel = newBreadcrumbPanel;

        // Add new components
        add(breadcrumbPanel);
        add(treeScrollPane, BorderLayout.CENTER);

        // Setup scroll listeners for new scroll pane
        setupScrollListenersForScrollPane(newScrollPane);

        // Perform initial setup
        performInitialSetup();

        // Refresh layout
        revalidate();
        repaint();
    }

    /**
     * Setup scroll listeners for a given scroll pane.
     * Extracted from setupScrollListeners() to support dynamic scroll pane replacement.
     */
    private void setupScrollListenersForScrollPane(JScrollPane scrollPane) {
        scrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (!e.getValueIsAdjusting()) {
                ScrollableTreePanel panel = (ScrollableTreePanel) scrollPane.getViewport().getView();
                panel.updateVisibleBlocksAndBreadcrumb();
            } else {
                ScrollableTreePanel panel = (ScrollableTreePanel) scrollPane.getViewport().getView();
                panel.updateVisibleBlocks();
            }
        });

        scrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                ScrollableTreePanel panel = (ScrollableTreePanel) scrollPane.getViewport().getView();
                panel.updateVisibleBlocks();
            }
        });
    }

    private void setupScrollListeners() {
    	treeScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
    		if(! e.getValueIsAdjusting()) {
    			treePanel.updateVisibleBlocksAndBreadcrumb();
    		}
    		else
    			treePanel.updateVisibleBlocks();
        });
        treeScrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
            	treePanel.updateVisibleBlocks();
            }
        });
    }

    private void performInitialSetup() {
        SwingUtilities.invokeLater(() -> {
            treePanel.updateVisibleBlocks();
            treePanel.requestFocusInWindow();
        });
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }
}