
package org.freeplane.view.swing.map.outline;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.freeplane.core.ui.components.FreeplaneToolBar;
import org.freeplane.core.ui.components.UITools;
import org.freeplane.core.ui.textchanger.TranslatedElementFactory;

class OutlinePane extends JPanel implements OutlineActionTargetProvider {
	private static final long serialVersionUID = 1L;
	private static final int SCROLL_INACTIVITY_DELAY_MS = 200;
	private JScrollPane treeScrollPane;
    private ScrollableTreePanel treePanel;
    private BreadcrumbPanel breadcrumbPanel;
    private JPopupMenu actionMenu;
    private OutlineActions actions;
    private OutlineController controller;
    private Rectangle lastBreadcrumbBounds;
	protected FreeplaneToolBar toolbar;

    OutlinePane(TreeNode rootNode) {
        this.breadcrumbPanel = new BreadcrumbPanel();

        this.treePanel = new ScrollableTreePanel(rootNode, breadcrumbPanel);
        breadcrumbPanel.setBackgroundColorSupplier(treePanel::getBackground);
        this.treeScrollPane = new JScrollPane(treePanel);
        UITools.setScrollbarIncrement(treeScrollPane);
        treePanel.setScrollPane(this.treeScrollPane);

        controller = new OutlineController(treePanel, treeScrollPane);
        breadcrumbPanel.initialize(controller, treePanel.getOutlineSelection());
        actions = new OutlineActions(this);
        actionMenu = actions.buildMenuLocalized();
		toolbar = new FreeplaneToolBar(SwingConstants.HORIZONTAL);
		configureToolbar(toolbar);
        JPanel topBarContainer = new JPanel(new BorderLayout());
        topBarContainer.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
        topBarContainer.add(toolbar, BorderLayout.WEST);

        setLayout(new BorderLayout(0, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public void layoutContainer(Container parent) {
                super.layoutContainer(parent);
                treeScrollPane.validate();
                Rectangle r = new Rectangle();
                Rectangle vpBorder = SwingUtilities.convertRectangle(
                        treeScrollPane,
                        treeScrollPane.getViewportBorderBounds(),
                        OutlinePane.this);
                Insets viewportInsets = treeScrollPane.getViewport().getInsets();

                int x = vpBorder.x + viewportInsets.left;
                int y = vpBorder.y + viewportInsets.top;
                int w = Math.max(0, vpBorder.width - viewportInsets.left - viewportInsets.right);
                int h = breadcrumbPanel.getPreferredBreadcrumbHeight();

                r.x = x;
                r.y = y;
                r.width = w;
                r.height = h;
                if (lastBreadcrumbBounds == null || !lastBreadcrumbBounds.equals(r)) {
                    lastBreadcrumbBounds = new Rectangle(r);
                }
                breadcrumbPanel.setBounds(r);
            }
        });

        add(topBarContainer, BorderLayout.NORTH);
        add(breadcrumbPanel);
        add(treeScrollPane, BorderLayout.CENTER);

        setupScrollListeners();
    }

	private void configureToolbar(final FreeplaneToolBar toolbar) {
        final JButton menuButton = new JButton("\u2261");
        TranslatedElementFactory.createTooltip(menuButton, "outline.menu.tooltip");
        menuButton.addActionListener(e -> {
            TreeNode selected = treePanel.getOutlineSelection().getSelectedNode();
            boolean hasParent = selected != null && selected.getParent() != null;
            boolean hasChild = selected != null && !selected.getChildren().isEmpty();
            boolean canToggle = selected != null && hasChild && selected.getLevel() > 0;
            actions.selectInMap.setEnabled(selected != null);
            actions.goParent.setEnabled(hasParent);
            actions.goChild.setEnabled(hasChild);
            actions.toggleExpand.setEnabled(canToggle);
            actionMenu.show(menuButton, 0, menuButton.getHeight());
        });
        toolbar.add(menuButton);
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
    	treePanel.rebuildFromNode(node);
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
        newBreadcrumbPanel.setBackgroundColorSupplier(newTreePanel::getBackground);
        JScrollPane newScrollPane = new JScrollPane(newTreePanel);
        UITools.setScrollbarIncrement(newScrollPane);

        newTreePanel.setScrollPane(newScrollPane);

        controller = new OutlineController(newTreePanel, newScrollPane);
        newBreadcrumbPanel.initialize(controller, newTreePanel.getOutlineSelection());

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

    @Override
    public OutlineActionTarget getTarget() { return controller; }

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
