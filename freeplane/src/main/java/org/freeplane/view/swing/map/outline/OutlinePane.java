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
    	treeScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
    		if(! e.getValueIsAdjusting()) {
    			BreadcrumbState state = treePanel.calculateBreadcrumbState();

    			if (state != null) {
    				breadcrumbPanel.update(state);
					if (state.levelReductionFirstVisibleNodeIndex >= 0) {
						treePanel.updateVisibleBlocks(state.levelReductionFirstVisibleNodeIndex);
						return;
					}
				}
    		}
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