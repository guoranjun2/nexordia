
package org.freeplane.view.swing.map.outline;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.freeplane.core.ui.components.UITools;

class OutlinePane extends JPanel {
	private static final long serialVersionUID = 1L;
	private static final int SCROLL_INACTIVITY_DELAY_MS = 200;
	private JScrollPane treeScrollPane;
    private ScrollableTreePanel treePanel;
    private BreadcrumbPanel breadcrumbPanel;

    OutlinePane(TreeNode rootNode) {
        this.breadcrumbPanel = new BreadcrumbPanel();

        this.treePanel = new ScrollableTreePanel(rootNode, breadcrumbPanel);
        this.treeScrollPane = new JScrollPane(treePanel);
        UITools.setScrollbarIncrement(treeScrollPane);
        treePanel.setScrollPane(this.treeScrollPane);

        OutlineController controller = new OutlineController(treePanel, treeScrollPane);
        breadcrumbPanel.initialize(controller, treePanel.getOutlineSelection());

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

    ScrollableTreePanel getTreePanel() {
        return treePanel;
    }

    void updateNodeTitle(TreeNode node) {
        SwingUtilities.invokeLater(() -> {
            treePanel.updateNodeTitle(node);
        });
    }

    void rebuildFromNode(TreeNode node) {
        SwingUtilities.invokeLater(() -> {
            treePanel.rebuildFromNode(node);
        });
    }

    void setRootNode(TreeNode newRootNode) {
        if (treePanel != null) {
            TreeNode oldRoot = treePanel.getRoot();
            if (oldRoot != null) {
                cleanupTree(oldRoot);
            }
        }

        remove(treeScrollPane);
        remove(breadcrumbPanel);

        BreadcrumbPanel newBreadcrumbPanel = new BreadcrumbPanel();

        ScrollableTreePanel newTreePanel = new ScrollableTreePanel(newRootNode, newBreadcrumbPanel);
        JScrollPane newScrollPane = new JScrollPane(newTreePanel);
        UITools.setScrollbarIncrement(newScrollPane);

        newTreePanel.setScrollPane(newScrollPane);

        OutlineController newController = new OutlineController(newTreePanel, newScrollPane);
        newBreadcrumbPanel.initialize(newController, newTreePanel.getOutlineSelection());

        this.treePanel = newTreePanel;
        this.treeScrollPane = newScrollPane;
        this.breadcrumbPanel = newBreadcrumbPanel;

        add(breadcrumbPanel);
        add(treeScrollPane, BorderLayout.CENTER);

        setupScrollListeners();

        performInitialSetup();

        revalidate();
        repaint();
    }

    private void setupScrollListeners() {
        final Timer scrollDebounceTimer = new Timer(SCROLL_INACTIVITY_DELAY_MS, e2 -> treePanel.updateVisibleBlocksAndBreadcrumb());
        scrollDebounceTimer.setRepeats(false);

		 treeScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> {
			 treePanel.updateVisibleBlocks();
			 scrollDebounceTimer.restart();
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
        });
    }

    @Override
    public boolean isOptimizedDrawingEnabled() {
        return false;
    }

    static void cleanupTree(TreeNode rootTreeNode) {
        if (rootTreeNode instanceof MapTreeNode) {
            ((MapTreeNode) rootTreeNode).cleanupListeners();
        }
    }


	boolean isSelected(TreeNode node) {
		return treePanel.getOutlineSelection().isSelected(node);
	}

	public void setSelected(TreeNode node) {
		treePanel.getOutlineSelection().selectNode(node);
	}
}
