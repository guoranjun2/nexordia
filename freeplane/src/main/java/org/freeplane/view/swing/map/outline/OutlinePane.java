
package org.freeplane.view.swing.map.outline;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

class OutlinePane extends JPanel {
	private static final long serialVersionUID = 1L;
	private JScrollPane treeScrollPane;
    private ScrollableTreePanel treePanel;
    private BreadcrumbPanel breadcrumbPanel;

    OutlinePane(TreeNode rootNode) {
        this.breadcrumbPanel = new BreadcrumbPanel();

        this.treePanel = new ScrollableTreePanel(rootNode, breadcrumbPanel);
        this.treeScrollPane = new JScrollPane(treePanel);

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


    void refreshTree() {
        SwingUtilities.invokeLater(() -> {
            treePanel.updateVisibleNodes();
        });
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
        if (newRootNode == null) {
            newRootNode = new TreeNode("No Data", "empty");
        }

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

        newTreePanel.setScrollPane(newScrollPane);

        OutlineController newController = new OutlineController(newTreePanel, newScrollPane);
        newBreadcrumbPanel.initialize(newController, newTreePanel.getOutlineSelection());

        this.treePanel = newTreePanel;
        this.treeScrollPane = newScrollPane;
        this.breadcrumbPanel = newBreadcrumbPanel;

        add(breadcrumbPanel);
        add(treeScrollPane, BorderLayout.CENTER);

        setupScrollListenersForScrollPane(newScrollPane);

        performInitialSetup();

        revalidate();
        repaint();
    }

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

        scrollPane.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
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
